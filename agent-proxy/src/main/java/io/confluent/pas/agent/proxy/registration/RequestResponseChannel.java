package io.confluent.pas.agent.proxy.registration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import io.confluent.pas.agent.common.services.schemas.Registration;
import io.confluent.pas.agent.common.utils.JsonUtils;
import io.confluent.pas.agent.common.services.models.Key;
import io.confluent.pas.agent.common.services.models.Request;
import io.confluent.pas.agent.common.services.models.Response;
import io.confluent.pas.agent.proxy.registration.kafka.ConsumerService;
import io.confluent.pas.agent.proxy.registration.schemas.RegistrationSchemas;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A channel for handling request-response patterns in a reactive messaging
 * system.
 * This class encapsulates the logic for:
 * <ul>
 * <li>Sending requests to a server</li>
 * <li>Tracking requests with correlation IDs and request indices</li>
 * <li>Processing responses asynchronously</li>
 * <li>Handling errors in the request-response cycle</li>
 * </ul>
 * The channel maintains a counter for request indices to uniquely identify each
 * request
 * and correlate it with its corresponding response.
 */
@Slf4j
@Builder
public class RequestResponseChannel implements AutoCloseable {

    /**
     * Interface for processing responses received from the server.
     * Implementors handle the response data and status based on the correlation ID
     * and request index.
     */
    public interface ResponseProcessor {
        /**
         * Process a response received from the server.
         *
         * @param channel       the channel through which the response was received
         * @param correlationId the unique ID for the request and response pairing
         * @param response      the response object containing the payload and status
         */
        void process(RequestResponseChannel channel,
                     String correlationId,
                     Response response);
    }

    // Atomic counter to ensure thread-safe request indexing
    private final AtomicInteger requestIndex = new AtomicInteger(0);
    // Unique identifier to correlate requests with their responses
    private final String correlationId;
    // Registration details for the service being communicated with
    private final Registration registration;
    // Handler for sending requests and registering for responses
    private final RequestResponseHandler requestResponseHandler;
    // Callback for processing received responses
    private final ResponseProcessor responseProcessor;
    // Schemas for validating request and response formats
    private final RegistrationSchemas schemas;

    /**
     * Creates a new request-response channel.
     *
     * @param correlationId          unique ID to correlate requests with responses
     * @param registration           service registration details
     * @param requestResponseHandler handler for sending requests and receiving
     *                               responses
     * @param processor              callback for processing received responses
     */
    public RequestResponseChannel(String correlationId,
                                  Registration registration,
                                  RequestResponseHandler requestResponseHandler,
                                  ResponseProcessor processor,
                                  RegistrationSchemas schemas) {
        this.correlationId = correlationId;
        this.registration = registration;
        this.requestResponseHandler = requestResponseHandler;
        this.responseProcessor = processor;
        this.schemas = schemas;

        // Register handlers for responses and errors immediately upon construction
        registerResponseHandler();
    }

    @Override
    public void close() {
        // Unregister the handlers when the channel is closed
        requestResponseHandler.unregisterHandler(registration, correlationId);
    }

    /**
     * Registers success and error handlers for responses with the request-response
     * handler.
     * This sets up the callback mechanism for processing responses.
     */
    private void registerResponseHandler() {
        requestResponseHandler.registerHandler(
                registration,
                correlationId,
                createSuccessResponseHandler(responseProcessor),
                createErrorResponseHandler(responseProcessor));
    }

    /**
     * Creates a handler for successful responses.
     * This handler deserializes the response and forwards it to the processor.
     *
     * @param processor callback to forward the processed response to
     * @return a response handler for successful responses
     */
    private ConsumerService.ResponseHandler createSuccessResponseHandler(ResponseProcessor processor) {
        return response -> {
            try {
                // Convert JsonNode to Response object for easier handling
                final Response responseMessage = JsonUtils.toObject(response.toString(), Response.class);
                processor.process(
                        this,
                        correlationId,
                        responseMessage);
            } catch (JsonProcessingException e) {
                // Handle JSON parsing errors by forwarding them as error responses
                log.error("Error parsing response", e);
                processor.process(
                        this,
                        correlationId,
                        new Response(e));
            }
        };
    }

    /**
     * Creates a handler for error responses or exceptions that occur during
     * processing.
     * This handler formats errors and forwards them to the processor.
     *
     * @param processor callback to forward the error information to
     * @return an error handler
     */
    private ConsumerService.ErrorHandler createErrorResponseHandler(ResponseProcessor processor) {
        return error -> {
            log.error("Error processing response", error);
            processor.process(
                    this,
                    correlationId,
                    new Response(error));
        };
    }

    /**
     * Send a request to the server and return a unique request index for tracking.
     * The request is assigned a unique index within this correlation ID's context.
     *
     * @param request the request payload as a map of key-value pairs
     * @return a mono that emits the request index for correlation with the response
     */
    public Mono<Integer> sendRequest(Map<String, Object> request) {
        // Get a unique index for this request (thread-safe via AtomicInteger)
        final int idx = this.requestIndex.getAndIncrement();
        // Create a request object with an index and payload
        final Request requestMessage = new Request(idx, request);

        return sendRequestToHandler(requestMessage, idx);
    }

    /**
     * Sends the prepared request to the handler and returns the request index.
     * Logs any errors that occur during sending.
     *
     * @param requestMessage the prepared request object
     * @param idx            the request index for correlation
     * @return a mono that emits the request index once the request is sent
     */
    private Mono<Integer> sendRequestToHandler(Request requestMessage, int idx) {
        final JsonNode envelope = schemas.getRequestSchema().envelope(requestMessage);

        return requestResponseHandler.sendRequest(
                        registration,
                        new Key(correlationId),
                        envelope)
                .doOnError(error -> log.error("Error sending request", error))
                .then(Mono.just(idx)); // Return the index after send completes
    }
}
