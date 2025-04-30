package io.confluent.pas.agent.proxy.frameworks.java;

import io.confluent.kafka.schemaregistry.json.JsonSchema;
import io.confluent.kafka.serializers.json.KafkaJsonSchemaDeserializer;
import io.confluent.kafka.serializers.json.KafkaJsonSchemaSerializer;
import io.confluent.pas.agent.common.services.KafkaConfiguration;
import io.confluent.pas.agent.common.services.KafkaPropertiesFactory;
import io.confluent.pas.agent.common.services.RegistrationService;
import io.confluent.pas.agent.common.services.schemas.Registration;
import io.confluent.pas.agent.common.services.schemas.RegistrationKey;
import io.confluent.pas.agent.proxy.frameworks.java.kafka.TopicManagement;
import io.confluent.pas.agent.proxy.frameworks.java.kafka.impl.TopicManagementImpl;
import io.confluent.pas.agent.proxy.frameworks.java.models.Key;
import io.confluent.pas.agent.proxy.frameworks.java.models.Request;
import io.confluent.pas.agent.proxy.frameworks.java.models.Response;
import io.confluent.pas.agent.proxy.frameworks.java.subscription.SubscriptionRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Produced;

import java.io.Closeable;
import java.util.Map;
import java.util.Properties;
import java.util.function.Supplier;

/**
 * Manages Kafka subscriptions for request-response communication patterns.
 * This class handles:
 * - Topic creation and management for request/response flows
 * - Schema registration with the Schema Registry
 * - Setting up Kafka Streams topology for processing requests
 * - Lifecycle management of Kafka resources
 *
 * @param <REQ> Request payload type
 * @param <RES> Response payload type
 */
@Slf4j
public class SubscriptionHandler<REQ, RES> implements Closeable {

    /**
     * Supplier for creating Kafka Streams instances.
     */
    public interface KStreamsSupplier {
        KafkaStreams get(Topology topology, Properties kStreamsProperties);
    }

    /**
     * Interface for handling incoming requests from Kafka topics.
     *
     * @param <REQ> Request payload type
     * @param <RES> Response payload type
     */
    public interface RequestHandler<REQ, RES> {
        void onRequest(SubscriptionRequest<REQ, RES> subscriptionRequest);
    }

    private final KafkaConfiguration kafkaConfiguration;
    private final RegistrationService<RegistrationKey, Registration> registrationService;
    private final Class<REQ> requestClass;
    private final Class<RES> responseClass;
    private final Serdes.WrapperSerde<Key> keySerde;
    private final Serdes.WrapperSerde<Request> requestSerde;
    private final Serdes.WrapperSerde<Response> responseSerde;
    private final Supplier<TopicManagement> topicManagementSupplier;
    private final KStreamsSupplier kafkaStreamsSupplier;
    private KafkaStreams kafkaStreams;

    /**
     * Creates a new subscription handler with the specified message types.
     *
     * @param kafkaConfiguration Kafka cluster configuration
     * @param requestClass       Class type for request payloads
     * @param responseClass      Class type for response payloads
     */
    public SubscriptionHandler(KafkaConfiguration kafkaConfiguration,
                               Class<REQ> requestClass,
                               Class<RES> responseClass) {
        this(kafkaConfiguration,
                requestClass,
                responseClass,
                new RegistrationService<>(
                        kafkaConfiguration,
                        RegistrationKey.class,
                        Registration.class),
                () -> new TopicManagementImpl(kafkaConfiguration),
                KafkaStreams::new
        );
    }

    /**
     * Creates a new subscription handler with the specified message types.
     *
     * @param kafkaConfiguration  Kafka cluster configuration
     * @param requestClass        Class type for request payloads
     * @param responseClass       Class type for response payloads
     * @param registrationService Registration service for storing capabilities
     */
    public SubscriptionHandler(KafkaConfiguration kafkaConfiguration,
                               Class<REQ> requestClass,
                               Class<RES> responseClass,
                               RegistrationService<RegistrationKey, Registration> registrationService,
                               Supplier<TopicManagement> topicManagementSupplier,
                               KStreamsSupplier kafkaStreamsSupplier) {
        this.kafkaConfiguration = kafkaConfiguration;
        this.requestClass = requestClass;
        this.responseClass = responseClass;
        this.registrationService = registrationService;
        this.keySerde = createSerde(Key.class, true);
        this.requestSerde = createSerde(Request.class, false);
        this.responseSerde = createSerde(Response.class, false);
        this.topicManagementSupplier = topicManagementSupplier;
        this.kafkaStreamsSupplier = kafkaStreamsSupplier;
    }

    /**
     * Subscribes to a registration using derived schemas from class types.
     *
     * @param registration Registration containing topic and name information
     * @param handler      Handler to process incoming requests
     * @throws SubscriptionException if subscription setup fails
     */
    public void subscribeWith(Registration registration,
                              RequestHandler<REQ, RES> handler) throws SubscriptionException {
        log.info("Subscribing for registration: {}", registration.getName());

        try {
            createTopics(registration, requestClass, responseClass);
            startSubscription(registration, handler);
        } catch (Exception e) {
            throw new SubscriptionException("Failed to subscribe with registration: " + registration.getName(), e);
        }
    }

    /**
     * Subscribes to a registration using explicit JSON
     *
     * @param registration   Registration containing topic and name information
     * @param requestSchema  Schema for request validation
     * @param responseSchema Schema for response validation
     * @param handler        Handler to process incoming requests
     * @throws SubscriptionException if subscription setup fails
     */
    public void subscribeWith(Registration registration,
                              JsonSchema requestSchema,
                              JsonSchema responseSchema,
                              RequestHandler<REQ, RES> handler) throws SubscriptionException {
        log.info("Subscribing for registration with custom schemas: {}", registration.getName());

        try {
            createTopicsWithSchemas(registration, requestSchema, responseSchema);
            startSubscription(registration, handler);
        } catch (Exception e) {
            throw new SubscriptionException("Failed to subscribe with schemas for: " + registration.getName(), e);
        }
    }

    /**
     * Releases all resources used by this handler.
     */
    @Override
    public void close() {
        log.info("Closing subscription handler resources");

        if (kafkaStreams != null) {
            try {
                kafkaStreams.close();
                log.debug("Kafka Streams closed successfully");
            } catch (Exception e) {
                log.warn("Error closing Kafka Streams", e);
            }
        }

        try {
            registrationService.close();
            log.debug("Registration service closed successfully");
        } catch (Exception e) {
            log.warn("Error closing registration service", e);
        }
    }

    /**
     * Creates a Kafka Serde for serialization/deserialization.
     *
     * @param valueClass Class to create serde for
     * @param isKey      Whether this is for a key (true) or value (false)
     * @return Configured Serde instance
     */
    private <T> Serdes.WrapperSerde<T> createSerde(Class<T> valueClass, boolean isKey) {
        final Map<String, Object> configuration =
                KafkaPropertiesFactory.getSchemaRegistryConfig(kafkaConfiguration, valueClass, isKey);

        final Serdes.WrapperSerde<T> serde = new Serdes.WrapperSerde<>(
                new KafkaJsonSchemaSerializer<>(),
                new KafkaJsonSchemaDeserializer<>());

        serde.configure(configuration, isKey);
        return serde;
    }

    /**
     * Creates topics using class types.
     */
    private <T, U> void createTopics(Registration registration,
                                     Class<T> requestClass,
                                     Class<U> responseClass) throws Exception {

        final JsonSchema reqSchema = Request.getSchema(requestClass);
        final JsonSchema resSchema = Response.getSchema(responseClass);

        try (TopicManagement topicManagement = topicManagementSupplier.get()) {
            topicManagement.createTopic(registration.getRequestTopicName(), Key.class, reqSchema);
            topicManagement.createTopic(registration.getResponseTopicName(), Key.class, resSchema);
            log.debug("Created topics for registration: {}", registration.getName());
        }
    }

    /**
     * Creates topics using explicit
     */
    private void createTopicsWithSchemas(Registration registration,
                                         JsonSchema requestSchema,
                                         JsonSchema responseSchema) throws Exception {
        final JsonSchema reqSchema = Request.getSchema(requestSchema);
        final JsonSchema resSchema = Response.getSchema(responseSchema);

        try (TopicManagement topicManagement = topicManagementSupplier.get()) {
            topicManagement.createTopic(registration.getRequestTopicName(), Key.class, reqSchema);
            topicManagement.createTopic(registration.getResponseTopicName(), Key.class, resSchema);
            log.debug("Created topics with schemas for registration: {}", registration.getName());
        }
    }

    /**
     * Registers capability and starts Kafka Streams processing.
     */
    private void startSubscription(Registration registration,
                                   RequestHandler<REQ, RES> handler) {
        registerCapability(registration);
        setupAndStartKafkaStreams(registration, handler);
    }

    /**
     * Registers the capability in the registration service.
     */
    private void registerCapability(Registration registration) {
        final RegistrationKey registrationKey = new RegistrationKey(registration.getName());

        if (!registrationService.isRegistered(registrationKey)) {
            log.info("Registering capability: {}", registration.getName());
            registrationService.register(registrationKey, registration);
        } else {
            log.info("Capability already registered: {}", registration.getName());
        }
    }

    /**
     * Sets up and starts the Kafka Streams topology.
     */
    private void setupAndStartKafkaStreams(Registration registration,
                                           RequestHandler<REQ, RES> handler) {
        StreamsBuilder builder = new StreamsBuilder();

        builder.stream(registration.getRequestTopicName(), Consumed.with(keySerde, requestSerde))
                .process(new SubscriptionHandlerSupplier<>(handler, requestClass))
                .to(registration.getResponseTopicName(), Produced.with(keySerde, responseSerde));

        final Topology topology = builder.build();
        kafkaStreams = kafkaStreamsSupplier.get(topology,
                KafkaPropertiesFactory.getKStreamsProperties(kafkaConfiguration));

        log.info("Starting Kafka Streams for registration: {}", registration.getName());
        kafkaStreams.start();
    }
}