package io.confluent.pas.agent.proxy.rest.a2a;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.confluent.pas.agent.common.utils.ResourceUtils;
import io.confluent.pas.agent.proxy.rest.a2a.schemas.AgentCard;
import io.confluent.pas.agent.common.services.schemas.Registration;
import io.confluent.pas.agent.common.services.schemas.ResourceRegistration;
import io.confluent.pas.agent.proxy.rest.RestUtils;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.responses.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
public class A2AAsyncServer {
    private final static String WELL_KNOWN_PATH = ".well-known";
    private final static String AGENT_PATH = "/a2a/";
    private final static String CARD_SCHEMA = ResourceUtils.readResourceAsString("schemas/agent-card.json");

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

    public Iterable<AgentCard> getAgentCards() {
        return a2ARegistry.getAgentCards();
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

    /**
     * Builds OpenAPI path items from all registrations.
     * Converts registrations into appropriate GET or POST path items based on their type.
     *
     * @return Map of paths to their corresponding PathItem definitions
     */
    public Map<String, PathItem> buildPathsFromRegistrations() {
        // Add all .well-known paths
        Map<String, PathItem> wellKnownPaths = new HashMap<>();

        Map<String, PathItem> paths = registrations.values()
                .stream()
                .map(registrationItem -> {
                    final Registration registration = registrationItem.getKey();
                    if (registration instanceof ResourceRegistration resourceRegistration) {
                        Pair<String, PathItem> wellKnownPathItem = getWellKnownPathItem(registration);
                        if (wellKnownPathItem != null) {
                            wellKnownPaths.put(wellKnownPathItem.getKey(), wellKnownPathItem.getValue());
                        }

                        return Pair.of("", new PathItem());
                    } else {
                        Pair<String, PathItem> wellKnownPathItem = getWellKnownPathItem(registration);
                        if (wellKnownPathItem != null) {
                            wellKnownPaths.put(wellKnownPathItem.getKey(), wellKnownPathItem.getValue());
                        }

                        return Pair.of("", new PathItem());
                    }
                })
                .filter(obj -> false)
                .collect(Collectors.toMap(Pair::getKey, Pair::getValue));

        wellKnownPaths.putAll(paths);

        return wellKnownPaths;
    }


    /**
     * Creates a well-known path item for a given registration that provides agent card information.
     * The path follows the format: /a2a/{agentName}/.well-known/agent.json
     *
     * @param registration the Registration object containing agent information
     * @return a Pair containing the URL path and PathItem for the well-known endpoint,
     * or null if CARD_SCHEMA is not available or if there's an error parsing the schema
     */
    private Pair<String, PathItem> getWellKnownPathItem(Registration registration) {
        if (CARD_SCHEMA == null) {
            return null;
        }

        final PathItem pathItem = new PathItem();
        final String urlPath = String.format("%s%s/%s/agent.json", AGENT_PATH, registration.getName(), WELL_KNOWN_PATH);

        try {
            final ApiResponse response = RestUtils.createApiResponse(CARD_SCHEMA);

            Operation operation = new Operation()
                    .summary(urlPath)
                    .operationId(urlPath)
                    .description(registration.getDescription())
                    .responses(RestUtils.createSuccessResponse(response));

            pathItem.operation(PathItem.HttpMethod.GET, operation);

            // Process URI template parameters
            RestUtils.addPathParameters(pathItem, urlPath);

            return Pair.of(urlPath, pathItem);
        } catch (JsonProcessingException e) {
            log.error("Error parsing schema for path: {}", urlPath, e);
            return null;
        }
    }
}
