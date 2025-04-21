package io.confluent.pas.agent.proxy.frameworks.java;

import io.confluent.pas.agent.proxy.frameworks.java.models.Key;
import org.apache.kafka.streams.processor.api.Processor;
import org.apache.kafka.streams.processor.api.ProcessorContext;
import org.apache.kafka.streams.processor.api.Record;

public class SubscriptionHandlerProcessor<REQ, RES> implements Processor<Key, REQ, Key, RES> {

    private final SubscriptionHandler.RequestHandler<REQ, RES> subscriptionHandler;
    private ProcessorContext<Key, RES> context;

    public SubscriptionHandlerProcessor(SubscriptionHandler.RequestHandler<REQ, RES> subscriptionHandler) {
        this.subscriptionHandler = subscriptionHandler;
    }

    @Override
    public void init(ProcessorContext<Key, RES> context) {
        this.context = context;
    }

    @Override
    public void process(Record<Key, REQ> record) {
        final Request<REQ, RES> request = new Request<>(
                record.key(),
                record.value(),
                this::sendResponse);

        subscriptionHandler.onRequest(request);
    }

    void sendResponse(Response<RES> response) {
        context.forward(new Record<>(
                response.key(),
                response.response(),
                System.currentTimeMillis()));
    }
}
