package io.confluent.pas.agent.common.services.kstream;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.streams.processor.api.Processor;
import org.apache.kafka.streams.processor.api.ProcessorContext;
import org.apache.kafka.streams.processor.api.Record;

/**
 * A generic Kafka Streams processor that handles stream processing operations.
 *
 * @param <Key> The type of the record key
 * @param <In>  The type of the input record value
 * @param <Out> The type of the output record value
 */
@Slf4j
@RequiredArgsConstructor
public class StreamingProcessor<Key, In, Out> implements Processor<Key, In, Key, Out> {

    /**
     * Handler responsible for processing the streaming records
     */
    private final StreamingHandler<Key, In, Out> handler;
    /**
     * Processor context for forwarding processed records
     */
    private ProcessorContext<Key, Out> context;

    /**
     * Initializes the processor with the given context.
     *
     * @param context The processor context used for forwarding records
     */
    @Override
    public void init(ProcessorContext<Key, Out> context) {
        this.context = context;
    }

    /**
     * Processes the input record by delegating to the handler and forwarding the result.
     *
     * @param record The input record to process
     */
    @Override
    public void process(Record<Key, In> record) {
        try {
            handler.handle(record.key(), record.value(), out -> {
                log.debug("Forwarding record: {}", record);
                context.forward(record.withValue(out));
            });
        } catch (Exception e) {
            log.error("Error processing record: {}", record, e);
            // TODO: Handle error appropriately
        }
    }
}
