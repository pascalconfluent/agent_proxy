package io.confluent.pas.agent.proxy.rest.agents;

import io.confluent.pas.agent.common.services.schemas.*;
import io.confluent.pas.agent.common.utils.JsonUtils;
import io.confluent.pas.agent.common.utils.UriUtils;
import io.confluent.pas.agent.proxy.registration.schemas.RegistrationSchemas;
import io.swagger.v3.oas.models.PathItem;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * REST server implementation that handles asynchronous request processing.
 * This component serves as a central registry for agent tools and resources,
 * managing their lifecycle and handling incoming requests directed to them.
 * <p>
 * Key responsibilities:
 * - Registration of API endpoints (both standard tools and resource-based
 * endpoints)
 * - Dynamic request routing to appropriate handlers
 * - OpenAPI documentation generation for registered endpoints
 * - Error handling and response formatting
 * <p>
 * The server supports two main types of endpoints:
 * 1. Standard API endpoints (POST-based tool operations)
 * 2. Resource-based endpoints (GET-based resource access)
 */
@Slf4j
@Component
public class AgentAsyncServer {
    /**
     * Type reference for deserializing request bodies into Map objects
     */
    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE = new ParameterizedTypeReference<>() {
    };

    /**
     * Record that holds registration information for a tool or resource endpoint.
     * Contains all necessary information to handle requests and generate OpenAPI
     * documentation.
     *
     * @param registration The registration details including name, description, and
     *                     endpoint metadata
     * @param schemas      Schema definitions for request and response
     *                     validation/documentation
     * @param call         Callback function to handle the actual request processing
     *                     and produce a response
     */
    public record RegistrationsItem(Registration registration,
                                    RegistrationSchemas schemas,
                                    Function<Map<String, Object>, Mono<Map<String, Object>>> call) {
    }

    /**
     * Thread-safe map of all registered endpoints, keyed by registration name
     */
    private final Map<String, RegistrationsItem> registrations = new ConcurrentHashMap<>();

    /**
     * Generator for OpenAPI documentation
     */
    private final AgentOpenApiGenerator openApiGenerator;

    /**
     * Constructs a new AgentAsyncServer with an OpenAPI generator.
     *
     * @param openApiGenerator The generator for OpenAPI documentation
     */
    public AgentAsyncServer(AgentOpenApiGenerator openApiGenerator) {
        this.openApiGenerator = openApiGenerator;
    }

    /**
     * Retrieves all currently registered endpoints.
     *
     * @return A list of all registered endpoints
     */
    public List<RegistrationsItem> getRegistrations() {
        return new ArrayList<>(registrations.values());
    }

    /**
     * Checks if an endpoint with the given name is registered.
     *
     * @param registrationName The name of the registration to check
     * @return true if the endpoint is not registered, false otherwise
     */
    public boolean isNotRegistered(String registrationName) {
        return !registrations.containsKey(registrationName);
    }

    /**
     * Adds a new endpoint registration to the server.
     * This method registers a callback function that will be invoked when requests
     * arrive
     * for the specified endpoint.
     *
     * @param registration The registration details, containing endpoint metadata
     * @param schemas      Schema definitions for request/response validation
     * @param call         Callback function that will process requests for this
     *                     endpoint
     * @return Empty Mono indicating completion
     */
    public Mono<Void> addRegistration(Registration registration,
                                      RegistrationSchemas schemas,
                                      Function<Map<String, Object>, Mono<Map<String, Object>>> call) {
        registrations.put(registration.getName(), new RegistrationsItem(registration, schemas, call));
        return Mono.empty();
    }

    /**
     * Calls a registered endpoint using URL path parts.
     * This is primarily used for resource-based endpoints where the path components
     * after the tool name represent the resource identifier.
     *
     * @param registrationName Name of the registered endpoint to call
     * @param urlParts         List of URL path parts that identify the resource
     * @return Mono containing the response map from the endpoint handler
     */
    public Mono<Map<String, Object>> callRegistration(String registrationName, final List<String> urlParts) {
        final ResourceRequest resourceRequest = new ResourceRequest(StringUtils.join(urlParts, "/"));
        return callRegistration(registrationName, JsonUtils.toMap(resourceRequest));
    }

    /**
     * Calls a registered endpoint with a request payload.
     * This is the core method that delegates request handling to the appropriate
     * callback function.
     *
     * @param registrationName Name of the registered endpoint to call
     * @param request          Map containing the request data
     * @return Mono containing the response map or an error if the registration is
     * not found
     */
    public Mono<Map<String, Object>> callRegistration(String registrationName, Map<String, Object> request) {
        RegistrationsItem registration = registrations.get(registrationName);
        if (registration == null) {
            log.error("No registration found for registrationName: {}", registrationName);
            return Mono.error(
                    new IllegalArgumentException("No registration found for registrationName: " + registrationName));
        }

        return registration.call.apply(request);
    }

    /**
     * Builds OpenAPI path items from all registered endpoints using the OpenAPI
     * generator.
     *
     * @return Map of API paths to their corresponding PathItem definitions for
     * OpenAPI documentation
     */
    public Map<String, PathItem> buildPathsFromRegistrations() {
        return openApiGenerator.buildPathsFromRegistrations(getRegistrations());
    }

    /**
     * Processes a POST request for a tool endpoint.
     * This method handles the complete request lifecycle including validation,
     * invocation of the appropriate handler, and error handling.
     *
     * @param request The incoming server request
     * @return A Mono containing the server response
     */
    public Mono<ServerResponse> processRequest(ServerRequest request) {
        final String toolName = getToolName(request);
        if (StringUtils.isEmpty(toolName)) {
            return createErrorResponse(HttpStatus.BAD_REQUEST, "No tool name found in request path");
        }

        log.debug("Processing request for {}", toolName);

        if (isNotRegistered(toolName)) {
            return createErrorResponse(HttpStatus.BAD_REQUEST,
                    String.format("Tool '%s' is not registered", toolName));
        }

        return request.bodyToMono(MAP_TYPE)
                .doOnNext(arguments -> log.info("Received request for tool {}", toolName))
                .flatMap(arguments -> callRegistration(toolName, arguments))
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
     * Processes a GET request for a resource endpoint.
     * This method handles the complete request lifecycle for resource-based
     * endpoints,
     * extracting path parameters and forwarding them to the appropriate handler.
     *
     * @param request The incoming server request
     * @return A Mono containing the server response
     */
    public Mono<ServerResponse> processGetRequest(ServerRequest request) {
        final List<String> urlParts = UriUtils.extractUrlParts(request.path());
        final String requestPath = request.path();
        final String toolName = extractToolName(urlParts);

        if (StringUtils.isEmpty(toolName)) {
            return createErrorResponse(HttpStatus.BAD_REQUEST, "No tool name found in request path");
        }

        if (isNotRegistered(toolName)) {
            return createErrorResponse(HttpStatus.BAD_REQUEST,
                    String.format("Tool '%s' is not registered", toolName));
        }

        // Remove the tool name from the URL parts
        final List<String> parts = urlParts.stream()
                .skip(1)
                .toList();

        return callRegistration(toolName, parts)
                .doOnError(e -> log.error("Error sending request to resource {}: {}",
                        requestPath, e.getMessage(), e))
                .flatMap(this::createResourceResponse)
                .onErrorResume(e -> createErrorResponse(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        String.format("Error processing resource request: %s", e.getMessage())));
    }

    /**
     * Creates a server response from a resource response.
     * Handles content type resolution and response formatting based on the resource
     * type.
     *
     * @param response The raw response map from the resource handler
     * @return A Mono containing the formatted server response
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
     * Extracts the content from a resource response based on its type.
     * Supports both blob (binary) and text resource responses.
     *
     * @param response The resource response to extract content from
     * @return The extracted content as a string
     * @throws IllegalArgumentException if the response type is unsupported
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
     * Extracts the tool name from the request path using path variables.
     *
     * @param request The server request
     * @return The extracted tool name
     */
    private String getToolName(ServerRequest request) {
        return request.pathVariable("toolName");
    }

    /**
     * Extracts the tool name from a list of URL path parts.
     * The tool name is expected to be the first part of the path.
     *
     * @param urlParts The list of URL path parts
     * @return The extracted tool name, or null if the list is empty
     */
    private String extractToolName(List<String> urlParts) {
        return urlParts.isEmpty() ? null : urlParts.getFirst();
    }

    /**
     * Creates a standardized error response with the given status and message.
     * Logs the error and formats it as a JSON response with error details.
     *
     * @param status  The HTTP status code for the response
     * @param message The error message
     * @return A Mono containing the formatted error response
     */
    private Mono<ServerResponse> createErrorResponse(HttpStatus status, String message) {
        log.error(message);
        return ServerResponse.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("error", status.getReasonPhrase(), "message", message));
    }
}
