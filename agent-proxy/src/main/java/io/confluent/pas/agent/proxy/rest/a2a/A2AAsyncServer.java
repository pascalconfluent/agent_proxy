package io.confluent.pas.agent.proxy.rest.a2a;

import io.confluent.pas.agent.proxy.rest.a2a.schemas.AgentCard;
import io.confluent.pas.agent.common.services.schemas.Registration;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
public class A2AAsyncServer {
    private final A2ARegistry a2ARegistry = new A2ARegistry();
    private final Map<String, Pair<Registration, Function<Map<String, Object>, Mono<Map<String, Object>>>>> registrations = new ConcurrentHashMap<>();

    public Mono<Void> addRegistration(Registration registration,
                                      Function<Map<String, Object>, Mono<Map<String, Object>>> call) {
        a2ARegistry.registerAgent(registration);
        registrations.put(registration.getName(), Pair.of(registration, call));
        
        return Mono.empty();
    }

    public AgentCard getAgentCards(String name) {
        return a2ARegistry.getAgentCard(name);
    }

    public List<Registration> getRegistrations() {
        return registrations.values()
                .stream()
                .map(Pair::getKey)
                .collect(Collectors.toList());
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
