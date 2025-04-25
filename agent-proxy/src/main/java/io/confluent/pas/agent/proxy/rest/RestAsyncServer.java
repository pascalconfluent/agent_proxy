package io.confluent.pas.agent.proxy.rest;

import io.confluent.pas.agent.common.services.schemas.Registration;
import io.confluent.pas.agent.common.services.schemas.ResourceRequest;
import io.confluent.pas.agent.common.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * RestAsyncServer is a component that manages asynchronous REST registrations.
 * It allows adding registration handlers and calling them with request data.
 */
@Slf4j
@Component
public class RestAsyncServer {

    private final Map<String, Pair<Registration, Function<Map<String, Object>, Mono<Map<String, Object>>>>> registrations = new ConcurrentHashMap<>();

    /**
     * Adds a registration handler to the server.
     *
     * @param registration the registration object containing metadata
     * @param call         a function that takes a Map and returns a Mono of Map
     * @return a Mono indicating completion
     */
    public Mono<Void> addRegistration(Registration registration,
                                      Function<Map<String, Object>, Mono<Map<String, Object>>> call) {
        registrations.put(registration.getName(), new ImmutablePair<>(registration, call));
        return Mono.empty();
    }


    /**
     * Calls a registration handler using URL parts.
     *
     * @param registrationName the name of the registration to call
     * @param urlParts         the list of URL parts to be joined into a path
     * @return a Mono containing the response as a Map
     */
    public Mono<Map<String, Object>> callRegistration(String registrationName, final List<String> urlParts) {
        final ResourceRequest resourceRequest = new ResourceRequest(StringUtils.join(urlParts, "/"));
        return callRegistration(registrationName, JsonUtils.toMap(resourceRequest));
    }

    /**
     * Calls a registration handler with a request map.
     *
     * @param registrationName the name of the registration to call
     * @param request          the request parameters as a Map
     * @return a Mono containing the response as a Map
     * @throws IllegalArgumentException if no registration is found for the given name
     */
    public Mono<Map<String, Object>> callRegistration(String registrationName, Map<String, Object> request) {
        Pair<Registration, Function<Map<String, Object>, Mono<Map<String, Object>>>> registration = registrations.get(registrationName);
        if (registration == null) {
            log.error("No registration found for registrationName: {}", registrationName);
            return Mono.error(new IllegalArgumentException("No registration found for registrationName: " + registrationName));
        }

        return registration.getRight().apply(request);
    }
}
