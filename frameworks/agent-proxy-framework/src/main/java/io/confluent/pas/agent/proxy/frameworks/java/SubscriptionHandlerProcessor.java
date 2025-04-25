package io.confluent.pas.agent.proxy.frameworks.java;

import io.confluent.pas.agent.common.utils.JsonUtils;
import io.confluent.pas.agent.proxy.frameworks.java.models.Key;
import io.confluent.pas.agent.proxy.frameworks.java.models.Request;
import io.confluent.pas.agent.proxy.frameworks.java.models.Response;
import io.confluent.pas.agent.proxy.frameworks.java.models.ResponseStatus;
import io.confluent.pas.agent.proxy.frameworks.java.subscription.SubscriptionRequest;
import io.confluent.pas.agent.proxy.frameworks.java.subscription.SubscriptionResponse;
import org.apache.kafka.streams.processor.api.Processor;
import org.apache.kafka.streams.processor.api.ProcessorContext;
import org.apache.kafka.streams.processor.api.Record;

public class SubscriptionHandlerProcessor<REQ, RES> implements Processor<Key, Request, Key, Response> {

    private final SubscriptionHandler.RequestHandler<REQ, RES> subscriptionHandler;
    private final Class<REQ> requestClass;

    private ProcessorContext<Key, Response> context;

    public SubscriptionHandlerProcessor(SubscriptionHandler.RequestHandler<REQ, RES> subscriptionHandler,
                                        Class<REQ> requestClass) {
        this.subscriptionHandler = subscriptionHandler;
        this.requestClass = requestClass;
    }

    @Override
    public void init(ProcessorContext<Key, Response> context) {
        this.context = context;
    }

    @Override
    public void process(Record<Key, Request> record) {
        final REQ request = JsonUtils.toObject(record.value().getPayload(), requestClass);

        final SubscriptionRequest<REQ, RES> subscriptionRequest = new SubscriptionRequest<>(
                record.key(),
                request,
                this::sendResponse);

        subscriptionHandler.onRequest(subscriptionRequest);
    }

    void sendResponse(SubscriptionResponse<RES> subscriptionResponse) {
        final Response response = new Response();
        response.setStatus(ResponseStatus.COMPLETED);
        response.setPayload(JsonUtils.toMap(subscriptionResponse.response()));

        context.forward(new Record<>(
                subscriptionResponse.key(),
                response,
                System.currentTimeMillis()));
    }
}
