package io.confluent.pas.agent.common.services.kstream;

import io.confluent.kafka.serializers.json.KafkaJsonSchemaDeserializer;
import io.confluent.kafka.serializers.json.KafkaJsonSchemaSerializer;
import io.confluent.pas.agent.common.services.KafkaConfiguration;
import io.confluent.pas.agent.common.services.KafkaPropertiesFactory;
import io.confluent.pas.agent.common.services.Streaming;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Produced;

import java.util.Map;
import java.util.Properties;


/**
 * Implementation of the Streaming interface for Kafka Streams processing.
 *
 * @param <Key> The type of the message key
 * @param <In>  The type of the input message value
 * @param <Out> The type of the output message value
 */
@Slf4j
public class StreamingImpl<Key, In, Out> implements Streaming<Key, In, Out> {

    private KafkaStreams kafkaStreams;

    /**
     * Initializes the streaming process with the given configuration and topics.
     *
     * @param kafkaConfiguration Configuration for Kafka connection
     * @param inTopicName        Name of the input topic
     * @param outTopicName       Name of the output topic
     * @param streamingHandler   Handler for processing the stream
     */
    @Override
    public void init(KafkaConfiguration kafkaConfiguration,
                     String inTopicName,
                     String outTopicName,
                     StreamingHandler<Key, In, Out> streamingHandler) {
        this.kafkaStreams = setupAndStartKafkaStreams(
                kafkaConfiguration,
                inTopicName,
                outTopicName,
                streamingHandler);
    }

    /**
     * Starts the Kafka Streams processing if the streams instance is initialized.
     */
    @Override
    public void start() {
        if (kafkaStreams != null) {
            kafkaStreams.start();
            log.info("Kafka Streams started");
        }
    }

    /**
     * Closes the Kafka Streams instance and releases all resources.
     */
    public void close() {
        if (kafkaStreams != null) {
            kafkaStreams.close();
            log.info("Kafka Streams closed");
        }
    }

    /**
     * Creates a Serde (Serializer/Deserializer) for a specific class type.
     *
     * @param kafkaConfiguration Configuration for Kafka connection
     * @param valueClass         Class type for which to create the Serde
     * @param isKey              Whether this Serde is for a key or value
     * @return A configured WrapperSerde instance
     */
    private <T> Serdes.WrapperSerde<T> createSerde(KafkaConfiguration kafkaConfiguration,
                                                   Class<T> valueClass,
                                                   boolean isKey) {
        final Map<String, Object> configuration =
                KafkaPropertiesFactory.getSchemaRegistryConfig(kafkaConfiguration, valueClass, isKey);

        final Serdes.WrapperSerde<T> serde = new Serdes.WrapperSerde<>(
                new KafkaJsonSchemaSerializer<>(),
                new KafkaJsonSchemaDeserializer<>());

        serde.configure(configuration, isKey);
        return serde;
    }

    /**
     * Sets up and initializes a KafkaStreams instance with the specified configuration and topology.
     *
     * @param kafkaConfiguration Configuration for Kafka connection
     * @param inTopicName        Name of the input topic
     * @param outTopicName       Name of the output topic
     * @param streamingHandler   Handler for processing the stream
     * @return Configured KafkaStreams instance
     */
    private KafkaStreams setupAndStartKafkaStreams(KafkaConfiguration kafkaConfiguration,
                                                   String inTopicName,
                                                   String outTopicName,
                                                   StreamingHandler<Key, In, Out> streamingHandler) {
        final Serdes.WrapperSerde<Key> keySerde = createSerde(kafkaConfiguration,
                streamingHandler.getKeyClass(),
                true);
        final Serdes.WrapperSerde<In> inSerde = createSerde(
                kafkaConfiguration,
                streamingHandler.getInClass(),
                false);
        final Serdes.WrapperSerde<Out> outSerde = createSerde(kafkaConfiguration,
                streamingHandler.getOutClass(),
                false);

        StreamsBuilder builder = new StreamsBuilder();

        builder.stream(inTopicName, Consumed.with(keySerde, inSerde))
                .process(new StreamingSupplier<>(streamingHandler))
                .to(outTopicName, Produced.with(keySerde, outSerde));

        final Properties configuration = KafkaPropertiesFactory.getKStreamsProperties(kafkaConfiguration);
        final Topology topology = builder.build();
        kafkaStreams = new KafkaStreams(topology, configuration);

        return kafkaStreams;
    }
}
