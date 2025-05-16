package io.confluent.pas.agent.common.services;

import io.confluent.pas.agent.common.services.kstream.StreamingHandler;

/**
 * Interface for managing streaming operations with Kafka.
 *
 * @param <Key> The type of the message key
 * @param <In>  The type of the input message value
 * @param <Out> The type of the output message value
 */
public interface Streaming<Key, In, Out> extends AutoCloseable {

    /**
     * Initializes the streaming process with the given configuration and topics.
     *
     * @param kafkaConfiguration Configuration for Kafka connection
     * @param inTopicName        Name of the input topic
     * @param outTopicName       Name of the output topic
     * @param streamingHandler   Handler for processing the stream
     */
    void init(KafkaConfiguration kafkaConfiguration,
              String inTopicName,
              String outTopicName,
              StreamingHandler<Key, In, Out> streamingHandler);

    /**
     * Starts the streaming process.
     * This method initializes and begins the Kafka streams processing.
     */
    void start();
}
