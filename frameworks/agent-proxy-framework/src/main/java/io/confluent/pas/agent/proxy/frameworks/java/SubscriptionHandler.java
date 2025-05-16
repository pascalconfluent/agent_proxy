package io.confluent.pas.agent.proxy.frameworks.java;

import io.confluent.kafka.schemaregistry.json.JsonSchema;
import io.confluent.pas.agent.common.services.*;
import io.confluent.pas.agent.common.services.kstream.StreamingImpl;
import io.confluent.pas.agent.common.services.schemas.Registration;
import io.confluent.pas.agent.common.services.schemas.RegistrationKey;
import io.confluent.pas.agent.proxy.frameworks.java.kafka.TopicManagement;
import io.confluent.pas.agent.proxy.frameworks.java.kafka.impl.TopicManagementImpl;
import io.confluent.pas.agent.proxy.frameworks.java.models.Key;
import io.confluent.pas.agent.proxy.frameworks.java.models.Request;
import io.confluent.pas.agent.proxy.frameworks.java.models.Response;
import io.confluent.pas.agent.proxy.frameworks.java.subscription.SubscriptionRequest;
import lombok.extern.slf4j.Slf4j;

import java.io.Closeable;
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
    private final Supplier<TopicManagement> topicManagementSupplier;
    private final Streaming<Key, Request, Response> streaming;

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
                new StreamingImpl<>()
        );
    }

    /**
     * Creates a new subscription handler with the specified message types and registration service.
     *
     * @param kafkaConfiguration      Kafka cluster configuration
     * @param requestClass            Class type for request payloads
     * @param responseClass           Class type for response payloads
     * @param registrationService     Registration service for managing registrations
     * @param topicManagementSupplier Supplier for topic management
     * @param streaming               Streaming service for processing requests
     */
    public SubscriptionHandler(KafkaConfiguration kafkaConfiguration,
                               Class<REQ> requestClass,
                               Class<RES> responseClass,
                               RegistrationService<RegistrationKey, Registration> registrationService,
                               Supplier<TopicManagement> topicManagementSupplier,
                               Streaming<Key, Request, Response> streaming) {
        this.kafkaConfiguration = kafkaConfiguration;
        this.requestClass = requestClass;
        this.responseClass = responseClass;
        this.registrationService = registrationService;
        this.topicManagementSupplier = topicManagementSupplier;
        this.streaming = streaming;
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

        if (streaming != null) {
            try {
                streaming.close();
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
        streaming.init(kafkaConfiguration,
                registration.getRequestTopicName(),
                registration.getResponseTopicName(),
                new RequestResponseHandler<>(requestClass, responseClass, handler));
        streaming.start();
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

}