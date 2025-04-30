package io.confluent.pas.agent.proxy.registration.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import io.confluent.pas.agent.common.services.schemas.Registration;
import io.confluent.pas.agent.common.utils.AsyncUtils;
import io.confluent.pas.agent.common.utils.JsonUtils;
import io.confluent.pas.agent.common.services.models.Response;
import io.confluent.pas.agent.proxy.registration.RequestResponseHandler;
import io.confluent.pas.agent.proxy.registration.schemas.RegistrationSchemas;
import io.confluent.pas.agent.proxy.registration.RequestResponseChannel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

/**
 * Abstract base class for registration handlers.
 * Provides common functionality for handling registration requests.
 *
 * @param <REG> The registration type extending the base Registration class
 * @param <SRV> The server type that processes the registration
 */
@Slf4j
@AllArgsConstructor
public abstract class AbstractRegistrationHandler<REG extends Registration, SRV, RES> implements RegistrationHandler {
    private static final String ERROR_PROCESSING_RESPONSE = "Failed to process response";
    private static final String UNKNOWN_RESPONSE_STATUS = "Unknown response status";

    /**
     * The registration data to be processed
     */
    @Getter
    protected final REG registration;

    /**
     * Registration schemas used for validation
     */
    @Getter
    protected final RegistrationSchemas schemas;

    /**
     * Server instance that handles the registration
     */
    @Getter
    protected final SRV registrationServer;

    /**
     * Handler for processing registration requests and responses
     */
    protected final RequestResponseHandler requestResponseHandler;

    /**
     * Function to convert the response payload into a specific result type
     */
    private Function<JsonNode, RES> resultSupplier;


    /**
     * Creates a new RequestResponseChannel for handling communication between client and server.
     * The channel is configured with a unique correlation ID and response processor that
     * delegates to processResponse method.
     *
     * @param sink MonoSink that will receive the final result of type RES
     * @return configured RequestResponseChannel instance
     */
    protected RequestResponseChannel getNewChannel(MonoSink<RES> sink) {
        // Generate unique ID to correlate requests with responses
        final String correlationId = UUID.randomUUID().toString();

        // Build and configure a new channel with required components
        return RequestResponseChannel.builder()
                .correlationId(correlationId)
                .registration(registration)         // Registration details for the service
                .requestResponseHandler(requestResponseHandler)  // Handles request/response routing
                .schemas(schemas)                   // Schemas for request/response validation
                .responseProcessor((channel, id, response) ->
                        processResponse(channel, id, response, sink))  // Process responses
                .build();
    }


    /**
     * Processes a request asynchronously by creating a new channel and sending the request through it.
     * The method handles the request lifecycle, including channel creation, request sending, and cleanup.
     *
     * @param arguments Map of key-value pairs containing the request parameters
     * @return Mono that emits the result of type RES when the request completes
     */
    protected Mono<RES> onRequest(Map<String, Object> arguments) {
        return Mono.create(sink -> AsyncUtils.executeConsumer(
                        sink,
                        (resultSink) -> {
                            // Create new channel for handling request/response communication
                            RequestResponseChannel channel = getNewChannel(resultSink);

                            // Ensure that the channel is closed when the resultSink is disposed
                            resultSink.onDispose(channel::close);

                            // Send the request through the channel and handle success/error cases
                            channel.sendRequest(arguments)
                                    .doOnError(error -> log.error("Error processing request", error))
                                    .doOnSuccess((ignore) -> log.debug("Request sent successfully"))
                                    .block();
                        }
                )
        );
    }

    /**
     * Processes a response from the tool handler.
     * Routes the response to the appropriate handler based on status.
     *
     * @param channel       the channel through which the response was received
     * @param correlationId the correlation ID of the request
     * @param response      the response from the handler
     * @param sink          the sink to receive the result
     */
    protected void processResponse(RequestResponseChannel channel,
                                   String correlationId,
                                   Response response,
                                   MonoSink<RES> sink) {
        switch (response.getStatus()) {
            case ERROR:
                handleErrorResponse(correlationId, response, sink);
                break;
            case COMPLETED:
                handleCompletedResponse(response.getPayload(), sink);
                break;
            case INPUT_REQUIRED:
                // Reserved for future implementation of interactive tools
                log.debug("Tool requires additional input for request {}", correlationId);
                sink.error(new UnsupportedOperationException("INPUT_REQUIRED status not yet implemented"));
                break;
            default:
                log.error("Unknown response status {} for request {}", response.getStatus(), correlationId);
                sink.error(new RuntimeException(UNKNOWN_RESPONSE_STATUS));
        }
    }

    /**
     * Handles a successful response by creating appropriate tool contents.
     *
     * @param payload the response payload
     * @param sink    the sink to receive the result
     */
    private void handleCompletedResponse(Map<String, Object> payload, MonoSink<RES> sink) {
        try {
            sink.success(resultSupplier.apply(JsonUtils.toJsonNode(payload)));
        } catch (Exception e) {
            log.error(ERROR_PROCESSING_RESPONSE, e);
            sink.error(e);
        }
    }

    /**
     * Handles an error response by extracting error details and passing to the
     * sink.
     *
     * @param correlationId the correlation ID of the request
     * @param response      the response from the handler
     * @param sink          the sink to receive the error
     */
    private void handleErrorResponse(String correlationId,
                                     Response response,
                                     MonoSink<RES> sink) {
        final String errorMessage = response.getMessage();
        final Throwable exception = response.getException().toThrowable();
        log.error("Error processing request {}: {}", correlationId, errorMessage);

        sink.error(Objects.requireNonNullElseGet(exception, () -> new RuntimeException(errorMessage)));
    }
}
