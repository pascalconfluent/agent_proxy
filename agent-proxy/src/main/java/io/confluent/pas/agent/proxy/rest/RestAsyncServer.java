package io.confluent.pas.agent.proxy.rest;

import io.confluent.pas.agent.common.services.schemas.Registration;
import io.confluent.pas.agent.common.services.schemas.ResourceRequest;
import io.confluent.pas.agent.common.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Stream;

@Slf4j
@Component
public class RestAsyncServer {

    private final Map<String, Pair<Registration, Function<Map<String, Object>, Mono<Map<String, Object>>>>> registrations = new ConcurrentHashMap<>();

    public Mono<Void> addRegistration(Registration registration,
                                      Function<Map<String, Object>, Mono<Map<String, Object>>> call) {
        registrations.put(registration.getName(), new ImmutablePair<>(registration, call));
        return Mono.empty();
    }

    public Mono<Map<String, Object>> callRegistration(ServerRequest serverRequest, String name, Map<String, Object> request) {

        if (serverRequest.method() == HttpMethod.GET) {
            // Handle GET requests
            final List<String> urlParts = extractUrlParts(serverRequest);
            final String requestPath = serverRequest.path();

            log.debug("Processing resource request for path: {}", requestPath);
            final Pair<Registration, Function<Map<String, Object>, Mono<Map<String, Object>>>> registration = findResourceHandler(urlParts)
                    .orElseThrow(() -> new IllegalArgumentException("No registration found for path: " + requestPath));
            final ResourceRequest resourceRequest = new ResourceRequest(StringUtils.join(urlParts, "/"));

            return registration.
                    getRight()
                    .apply(JsonUtils.toMap(resourceRequest));
        }

        Pair<Registration, Function<Map<String, Object>, Mono<Map<String, Object>>>> registration = registrations.get(name);
        if (registration == null) {
            log.error("No registration found for name: {}", name);
            return Mono.error(new IllegalArgumentException("No registration found for name: " + name));
        }

        return registration.getRight().apply(request);
    }

    /**
     * Find the handler for the given URL parts.
     *
     * @param urlParts the URL parts
     * @return an Optional containing the handler if found, empty otherwise
     */
    private Optional<Pair<Registration, Function<Map<String, Object>, Mono<Map<String, Object>>>>> findResourceHandler(
            List<String> urlParts) {
        return registrations.keySet().stream()
                .filter(pattern -> isUrlPatternMatch(pattern, urlParts))
                .map(registrations::get)
                .findFirst();
    }

    /**
     * Extract the URL parts from the request path.
     *
     * @param request the server request
     * @return the list of URL parts
     */
    private List<String> extractUrlParts(ServerRequest request) {
        return Stream.of(request.path().split("/"))
                .filter(StringUtils::isNotEmpty)
                .skip(1) // Skip the initial part which is empty
                .toList();
    }

    /**
     * Check if the given URL pattern matches the URL parts.
     *
     * @param urlPattern the URL pattern
     * @param urlParts   the URL parts
     * @return true if the pattern matches the URL parts
     */
    private boolean isUrlPatternMatch(String urlPattern, List<String> urlParts) {
        final List<String> patternParts = Stream.of(urlPattern.split("/"))
                .filter(StringUtils::isNotEmpty)
                .toList();

        if (urlParts.size() != patternParts.size()) {
            return false;
        }

        return Stream.iterate(0, i -> i + 1)
                .limit(urlParts.size())
                .allMatch(i -> isPartMatch(patternParts.get(i), urlParts.get(i)));
    }

    /**
     * Check if a pattern part matches a URL part.
     *
     * @param patternPart the pattern part
     * @param urlPart     the URL part
     * @return true if the pattern part matches the URL part
     */
    private boolean isPartMatch(String patternPart, String urlPart) {
        return (patternPart.startsWith("{") && patternPart.endsWith("}")) ||
                patternPart.equals(urlPart);
    }
}
