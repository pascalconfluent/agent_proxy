package io.confluent.pas.agent.proxy.registration.handlers.rest;

import io.confluent.pas.agent.common.services.schemas.Registration;
import io.confluent.pas.agent.common.utils.JsonUtils;
import io.confluent.pas.agent.proxy.registration.RequestResponseHandler;
import io.confluent.pas.agent.proxy.registration.handlers.AbstractRegistrationHandler;
import io.confluent.pas.agent.proxy.registration.schemas.RegistrationSchemas;
import io.confluent.pas.agent.proxy.rest.RestAsyncServer;
import io.confluent.pas.agent.proxy.server.RequestResponseChannel;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

import java.util.Map;
import java.util.UUID;

@Slf4j
public class RestToolHandler extends AbstractRegistrationHandler<Registration, RestAsyncServer, Map<String, Object>> {

    public RestToolHandler(Registration registration,
                           RegistrationSchemas schemas,
                           RequestResponseHandler requestResponseHandler,
                           RestAsyncServer restAsyncServer) {
        super(registration, schemas, restAsyncServer, requestResponseHandler, JsonUtils::toMap);
    }

    @Override
    public Mono<Void> initialize() {
        return registrationServer.addRegistration(registration,
                (arguments) -> Mono.create(sink -> sendRequest(arguments, sink)));
    }

    @Override
    public Mono<Void> teardown() {
        return null;
    }

    private void sendRequest(Map<String, Object> arguments,
                             MonoSink<Map<String, Object>> sink) {
        final String correlationId = UUID.randomUUID().toString();

        RequestResponseChannel.builder()
                .correlationId(correlationId)
                .registration(registration)
                .requestResponseHandler(requestResponseHandler)
                .schemas(schemas)
                .responseProcessor((channel, id, response) -> processResponse(id, response, sink))
                .build()
                .sendRequest(arguments)
                .doOnError(sink::error)
                .block();
    }
}
