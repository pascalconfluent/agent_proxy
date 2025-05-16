package io.confluent.pas.agent.proxy.frameworks.java;

import io.confluent.pas.agent.common.services.kstream.StreamingHandler;
import io.confluent.pas.agent.common.utils.JsonUtils;
import io.confluent.pas.agent.proxy.frameworks.java.models.Key;
import io.confluent.pas.agent.proxy.frameworks.java.models.Request;
import io.confluent.pas.agent.proxy.frameworks.java.models.Response;
import io.confluent.pas.agent.proxy.frameworks.java.models.ResponseStatus;
import io.confluent.pas.agent.proxy.frameworks.java.subscription.SubscriptionRequest;
import io.confluent.pas.agent.proxy.frameworks.java.subscription.SubscriptionResponse;

import java.util.function.Consumer;

public record RequestResponseHandler<REQ, RES>(Class<REQ> requestClass, Class<RES> responseClass,
                                               SubscriptionHandler.RequestHandler<REQ, RES> subscriptionHandler)
        implements StreamingHandler<Key, Request, Response> {

    @Override
    public Class<Key> getKeyClass() {
        return Key.class;
    }

    @Override
    public Class<Request> getInClass() {
        return Request.class;
    }

    @Override
    public Class<Response> getOutClass() {
        return Response.class;
    }

    @Override
    public void handle(Key key, Request request, Consumer<Response> sink) {
        final REQ req = JsonUtils.toObject(request.getPayload(), requestClass);

        final SubscriptionRequest<REQ, RES> subscriptionRequest = new SubscriptionRequest<>(
                key,
                req,
                subscriptionResponse -> sendResponse(subscriptionResponse, sink));

        subscriptionHandler.onRequest(subscriptionRequest);
    }

    private void sendResponse(SubscriptionResponse<RES> subscriptionResponse, Consumer<Response> sink) {
        final Response response = new Response();
        response.setStatus(ResponseStatus.COMPLETED);
        response.setPayload(JsonUtils.toMap(subscriptionResponse.response()));

        sink.accept(response);
    }
}
