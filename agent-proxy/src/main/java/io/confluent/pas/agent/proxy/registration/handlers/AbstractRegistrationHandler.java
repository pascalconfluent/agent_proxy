package io.confluent.pas.agent.proxy.registration.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import io.confluent.pas.agent.common.services.schemas.Registration;
import io.confluent.pas.agent.common.utils.JsonUtils;
import io.confluent.pas.agent.proxy.frameworks.java.models.Response;
import io.confluent.pas.agent.proxy.registration.RequestResponseHandler;
import io.confluent.pas.agent.proxy.registration.schemas.RegistrationSchemas;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.MonoSink;

import java.util.Map;
import java.util.Objects;
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
public abstract class AbstractRegistrationHandler<REG extends Registration, SRV, RES> implements RegistrationHandler<SRV> {
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
    
    private Function<JsonNode, RES> resultSupplier;

    /**
     * Processes a response from the tool handler.
     * Routes the response to the appropriate handler based on status.
     *
     * @param correlationId the correlation ID of the request
     * @param response      the response from the handler
     * @param sink          the sink to receive the result
     */
    protected void processResponse(String correlationId,
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
