package io.confluent.pas.agent.proxy.rest;

import io.confluent.pas.agent.proxy.registration.RegistrationCoordinator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

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

    // Coordinator for managing registrations
    private final RegistrationCoordinator registrationCoordinator;

    private final RestAsyncServer asyncServer;

    /**
     * Constructor that initializes the controller and sets up resource handlers.
     *
     * @param registrationCoordinator the coordinator for managing registrations
     */
    public ToolRestController(RegistrationCoordinator registrationCoordinator,
                              RestAsyncServer asyncServer) {
        this.registrationCoordinator = registrationCoordinator;
        this.asyncServer = asyncServer;
    }


    /**
     * Process a request for a tool.
     *
     * @param request the server request
     * @return the server response
     */
    public Mono<ServerResponse> processRequest(ServerRequest request) {
        final String toolName = request.pathVariable("toolName");
        log.debug("Processing request for tool: {}", toolName);

        // Check if the tool is registered
        if (!registrationCoordinator.isRegistered(toolName)) {
            log.error("Tool {} is not registered", toolName);
            return createErrorResponse(
                    HttpStatus.BAD_REQUEST,
                    String.format("Tool '%s' is not registered", toolName));
        }

        return request.bodyToMono(MAP_TYPE)
                .doOnNext(arguments -> log.info("Received request for tool {}", toolName))
                .flatMap(arguments -> asyncServer.callRegistration(request, toolName, arguments))
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
     * Create an error response with the given status and message.
     *
     * @param status  the HTTP status
     * @param message the error message
     * @return the server response Mono
     */
    private Mono<ServerResponse> createErrorResponse(HttpStatus status, String message) {
        return ServerResponse.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("error", status.getReasonPhrase(), "message", message));
    }
}