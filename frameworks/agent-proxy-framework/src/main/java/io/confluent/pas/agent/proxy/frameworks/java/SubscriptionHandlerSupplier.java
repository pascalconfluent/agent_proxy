package io.confluent.pas.agent.proxy.frameworks.java;

import io.confluent.pas.agent.proxy.frameworks.java.models.Key;
import lombok.AllArgsConstructor;
import org.apache.kafka.streams.processor.api.Processor;
import org.apache.kafka.streams.processor.api.ProcessorSupplier;

@AllArgsConstructor
public class SubscriptionHandlerSupplier<REQ, RES> implements ProcessorSupplier<Key, REQ, Key, RES> {

    private final SubscriptionHandler.RequestHandler<REQ, RES> subscriptionHandler;

    @Override
    public Processor<Key, REQ, Key, RES> get() {
        return new SubscriptionHandlerProcessor<>(subscriptionHandler);
    }
}
