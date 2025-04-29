package io.confluent.pas.agent.proxy.registration.handlers.a2a;

import io.confluent.pas.agent.common.services.schemas.Registration;
import io.confluent.pas.agent.common.utils.JsonUtils;
import io.confluent.pas.agent.proxy.registration.RequestResponseHandler;
import io.confluent.pas.agent.proxy.registration.handlers.AbstractRegistrationHandler;
import io.confluent.pas.agent.proxy.registration.schemas.RegistrationSchemas;
import io.confluent.pas.agent.proxy.rest.a2a.A2AAsyncServer;
import reactor.core.publisher.Mono;

import java.util.Map;

public class A2AHandler extends AbstractRegistrationHandler<Registration, A2AAsyncServer, Map<String, Object>> {

    public A2AHandler(Registration registration,
                      RegistrationSchemas schemas,
                      RequestResponseHandler requestResponseHandler,
                      A2AAsyncServer asyncServer) {
        super(registration, schemas, asyncServer, requestResponseHandler, JsonUtils::toMap);
    }

    @Override
    public Mono<Void> initialize() {
        return registrationServer.addRegistration(
                registration,
                this::onRequest);
    }

    @Override
    public Mono<Void> teardown() {
        return Mono.empty();
    }

}
