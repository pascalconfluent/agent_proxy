package io.confluent.pas.agent.proxy.rest;

import io.confluent.pas.agent.common.services.schemas.*;
import io.confluent.pas.agent.common.utils.JsonUtils;
import io.confluent.pas.agent.common.utils.UriUtils;
import io.confluent.pas.agent.proxy.registration.RegistrationCoordinator;
import io.confluent.pas.agent.proxy.registration.handlers.CompositeHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller responsible for handling tool and resource requests.
 */
@Slf4j
@Component
public class ToolRestController {

    // Type reference for deserializing request a body to a Map
    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE = new ParameterizedTypeReference<>() {
    };

    private final RegistrationCoordinator registrationCoordinator;
    private final RestAsyncServer asyncServer;
    private final Map<String, String> resourceRegistrations = new HashMap<>();

    /**
     * Constructor that initializes the controller and sets up resource handlers.
     *
     * @param registrationCoordinator the coordinator for managing registrations
     * @param asyncServer             the server for handling asynchronous requests
     */
    public ToolRestController(RegistrationCoordinator registrationCoordinator,
            RestAsyncServer asyncServer) {
        this.registrationCoordinator = registrationCoordinator;
        this.asyncServer = asyncServer;
        initializeResourceNames();
    }

    /**
     * Process a request for a tool.
     *
     * @param request the server request
     * @return the server response
     */
    public Mono<ServerResponse> processRequest(ServerRequest request) {
        final String toolName = getToolName(request);
        if (StringUtils.isEmpty(toolName)) {
            return createErrorResponse(HttpStatus.BAD_REQUEST, "No tool name found in request path");
        }

        log.debug("Processing request for {}", toolName);

        if (!isToolRegistered(toolName)) {
            return createErrorResponse(HttpStatus.BAD_REQUEST,
                    String.format("Tool '%s' is not registered", toolName));
        }

        return request.bodyToMono(MAP_TYPE)
                .doOnNext(arguments -> log.info("Received request for tool {}", toolName))
                .flatMap(arguments -> asyncServer.callRegistration(toolName, arguments))
                .doOnError(e -> log.error("Error processing request for tool {}: {}",
                        toolName, e.getMessage(), e))
                .flatMap(response -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(response))
                .onErrorResume(e -> createErrorResponse(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        String.format("Error processing request for tool '%s': %s",
                                toolName, e.getMessage())));
    }

    /**
     * Process a GET request for a resource.
     *
     * @param request the server request
     * @return the server response
     */
    public Mono<ServerResponse> processGetRequest(ServerRequest request) {
        final List<String> urlParts = UriUtils.extractUrlParts(request.path());
        final String requestPath = request.path();

        final String toolName = lookupToolName(urlParts, requestPath);
        if (StringUtils.isEmpty(toolName)) {
            return createErrorResponse(HttpStatus.BAD_REQUEST, "No tool name found in request path");
        }

        if (!isToolRegistered(toolName)) {
            return createErrorResponse(HttpStatus.BAD_REQUEST,
                    String.format("Tool '%s' is not registered", toolName));
        }

        return asyncServer.callRegistration(toolName, urlParts)
                .doOnError(e -> log.error("Error sending request to resource {}: {}",
                        requestPath, e.getMessage(), e))
                .flatMap(this::createResourceResponse)
                .onErrorResume(e -> createErrorResponse(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        String.format("Error processing resource request: %s", e.getMessage())));
    }

    /**
     * Checks if a tool is registered with the registration coordinator.
     *
     * @param toolName the name of the tool
     * @return true if the tool is registered, false otherwise
     */
    private boolean isToolRegistered(String toolName) {
        if (!registrationCoordinator.isRegistered(toolName)) {
            log.error("Tool {} is not registered", toolName);
            return false;
        }
        return true;
    }

    /**
     * Create a server response from the resource response.
     *
     * @param response the resource response
     * @return the server response Mono
     */
    private Mono<ServerResponse> createResourceResponse(Map<String, Object> response) {
        final ResourceResponse resourceResponse = JsonUtils.toObject(response, ResourceResponse.class);
        final MediaType mediaType = MediaType.parseMediaType(resourceResponse.getMimeType());
        final String responseContent = extractResponseContent(resourceResponse);

        return ServerResponse.ok()
                .contentType(mediaType)
                .bodyValue(responseContent);
    }

    /**
     * Extract the content from the resource response.
     *
     * @param response the resource response
     * @return the response content as a string
     */
    private String extractResponseContent(ResourceResponse response) {
        if (response instanceof BlobResourceResponse blobResponse) {
            return blobResponse.getBlob();
        } else if (response instanceof TextResourceResponse textResponse) {
            return textResponse.getText();
        } else {
            throw new IllegalArgumentException("Unsupported resource response type: " + response.getClass().getName());
        }
    }

    /**
     * Initializes the resource handlers map based on registered handlers.
     */
    private void initializeResourceNames() {
        registrationCoordinator.getAllRegistrationHandlers()
                .stream()
                .filter(CompositeHandler::isResourceRegistration)
                .map(CompositeHandler::getRegistration)
                .forEach(registration -> {
                    final String urlPattern = ((ResourceRegistration) registration).getUrl();
                    resourceRegistrations.put(urlPattern, registration.getName());
                    log.debug("Registered resource handler for pattern: {}", urlPattern);
                });
    }

    /**
     * Looks up the tool name based on the URL parts and request path.
     *
     * @param urlParts    the URL parts
     * @param requestPath the request path
     * @return the tool name if found, null otherwise
     */
    private String lookupToolName(List<String> urlParts, String requestPath) {
        log.debug("Looking up tool name for path: {}", requestPath);

        return resourceRegistrations.keySet().stream()
                .filter(pattern -> UriUtils.isUrlPatternMatch(pattern, urlParts))
                .map(resourceRegistrations::get)
                .findFirst()
                .orElse(null);
    }

    /**
     * Extracts the tool name from the request path.
     *
     * @param request the server request
     * @return the tool name
     */
    private String getToolName(ServerRequest request) {
        return request.pathVariable("toolName");
    }

    /**
     * Create an error response with the given status and message.
     *
     * @param status  the HTTP status
     * @param message the error message
     * @return the server response Mono
     */
    private Mono<ServerResponse> createErrorResponse(HttpStatus status, String message) {
        log.error(message);
        return ServerResponse.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("error", status.getReasonPhrase(), "message", message));
    }
}