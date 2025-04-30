package io.confluent.pas.agent.proxy.frameworks.java;

import io.confluent.pas.agent.proxy.frameworks.java.models.Key;
import io.confluent.pas.agent.proxy.frameworks.java.models.Request;
import io.confluent.pas.agent.proxy.frameworks.java.models.Response;
import lombok.AllArgsConstructor;
import org.apache.kafka.streams.processor.api.Processor;
import org.apache.kafka.streams.processor.api.ProcessorSupplier;

@AllArgsConstructor
public class SubscriptionHandlerSupplier<REQ, RES> implements ProcessorSupplier<Key, Request, Key, Response> {

    private final SubscriptionHandler.RequestHandler<REQ, RES> subscriptionHandler;
    private final Class<REQ> requestClass;

    @Override
    public Processor<Key, Request, Key, Response> get() {
        return new SubscriptionHandlerProcessor<>(subscriptionHandler, requestClass);
    }
}
