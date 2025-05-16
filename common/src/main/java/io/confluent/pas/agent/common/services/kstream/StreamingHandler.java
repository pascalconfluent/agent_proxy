package io.confluent.pas.agent.common.services.kstream;

import reactor.core.publisher.Mono;

import java.util.function.Consumer;

/**
 * Interface defining handler for processing streaming data.
 * Implementations of this interface handle the processing logic for Kafka Streams.
 *
 * @param <Key> The type of the message key
 * @param <In>  The type of the input message value
 * @param <Out> The type of the output message value
 */
public interface StreamingHandler<Key, In, Out> {

    /**
     * Gets the Class object representing the key type.
     *
     * @return The Class object for the key type
     */
    Class<Key> getKeyClass();

    /**
     * Gets the Class object representing the input type.
     *
     * @return The Class object for the input type
     */
    Class<In> getInClass();

    /**
     * Gets the Class object representing the output type.
     *
     * @return The Class object for the output type
     */
    Class<Out> getOutClass();


    /**
     * Handles the processing of a single message in the stream.
     *
     * @param key  The key of the message to be processed
     * @param in   The input value to be processed
     * @param sink Consumer function to emit the processed output value
     */
    void handle(Key key, In in, Consumer<Out> sink);

}
