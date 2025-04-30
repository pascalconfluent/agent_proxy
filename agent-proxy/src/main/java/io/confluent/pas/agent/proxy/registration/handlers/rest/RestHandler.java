package io.confluent.pas.agent.proxy.registration.handlers.rest;

import io.confluent.pas.agent.common.services.schemas.Registration;
import io.confluent.pas.agent.common.utils.JsonUtils;
import io.confluent.pas.agent.proxy.registration.RequestResponseHandler;
import io.confluent.pas.agent.proxy.registration.handlers.AbstractRegistrationHandler;
import io.confluent.pas.agent.proxy.registration.schemas.RegistrationSchemas;
import io.confluent.pas.agent.proxy.rest.agents.AgentAsyncServer;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.Map;

@Slf4j
public class RestHandler extends AbstractRegistrationHandler<Registration, AgentAsyncServer, Map<String, Object>> {

    public RestHandler(Registration registration,
                       RegistrationSchemas schemas,
                       RequestResponseHandler requestResponseHandler,
                       AgentAsyncServer agentAsyncServer) {
        super(registration, schemas, agentAsyncServer, requestResponseHandler, JsonUtils::toMap);
    }

    @Override
    public Mono<Void> initialize() {
        return registrationServer.addRegistration(
                registration,
                schemas,
                this::onRequest);
    }

    @Override
    public Mono<Void> teardown() {
        return null;
    }
}
