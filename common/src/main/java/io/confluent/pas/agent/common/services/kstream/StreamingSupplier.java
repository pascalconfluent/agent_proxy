package io.confluent.pas.agent.common.services.kstream;

import lombok.AllArgsConstructor;
import org.apache.kafka.streams.processor.api.Processor;
import org.apache.kafka.streams.processor.api.ProcessorSupplier;


/**
 * A supplier class for streaming processors that handles the creation of processor instances
 * based on a given streaming handler.
 *
 * @param <Key> the type of the message key
 * @param <In>  the type of the input value
 * @param <Out> the type of the output value
 */
@AllArgsConstructor
public class StreamingSupplier<Key, In, Out> implements ProcessorSupplier<Key, In, Key, Out> {

    /**
     * The streaming handler that will be used by the created processors
     * to process the input messages.
     */
    private final StreamingHandler<Key, In, Out> streamingHandler;

    /**
     * Creates a new processor instance using the configured streaming handler.
     *
     * @return a new processor instance that will use the configured streaming handler
     */
    @Override
    public Processor<Key, In, Key, Out> get() {
        return new StreamingProcessor<>(streamingHandler);
    }
}
