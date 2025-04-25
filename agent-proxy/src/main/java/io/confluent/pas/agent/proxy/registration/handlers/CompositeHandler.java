package io.confluent.pas.agent.proxy.registration.handlers;

import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import io.confluent.pas.agent.common.services.schemas.Registration;
import io.confluent.pas.agent.common.services.schemas.ResourceRegistration;
import io.confluent.pas.agent.proxy.registration.RegistrationCoordinator;
import io.confluent.pas.agent.proxy.registration.RequestResponseHandler;
import io.confluent.pas.agent.proxy.registration.handlers.mcp.McpResourceHandler;
import io.confluent.pas.agent.proxy.registration.handlers.mcp.McpToolHandler;
import io.confluent.pas.agent.proxy.registration.handlers.rest.RestToolHandler;
import io.confluent.pas.agent.proxy.registration.schemas.RegistrationSchemas;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * A composite handler that manages registration and unregistration of multiple
 * handlers.
 * This class serves as a facade for different types of handlers and delegates
 * operations to them.
 * It supports both resource-type registrations and agent-type registrations.
 * <p>
 * The composite pattern allows treating individual handlers and compositions
 * uniformly
 * while providing centralized management of the registration lifecycle.
 */
@Slf4j
public class CompositeHandler implements RegistrationHandler<RegistrationCoordinator> {

    /**
     * List of managed registration handlers that will be initialized and torn down
     * together
     */
    private final List<RegistrationHandler<?>> handlers = new ArrayList<>();

    /**
     * The registration configuration that defines the behavior of this composite
     * handler
     */
    @Getter
    private final Registration registration;

    /**
     * Handler for processing requests and responses between the agent and clients
     */
    private final RequestResponseHandler requestResponseHandler;

    /**
     * Schemas associated with the registration for serialization/deserialization
     */
    @Getter
    private final RegistrationSchemas schemas;

    /**
     * The coordinator that manages the lifecycle of registrations
     */
    @Getter
    private final RegistrationCoordinator coordinator;

    @Getter
    private final boolean isResourceRegistration;

    /**
     * Constructs a new CompositeHandler with the specified components.
     *
     * @param registration           The registration configuration
     * @param requestResponseHandler Handler for processing requests and responses
     * @param schemaRegistryClient   Client for schema registry interactions
     * @param coordinator            The registration coordinator
     * @throws RestClientException If there's an error communicating with the schema
     *                             registry
     * @throws IOException         If there's an error reading or writing data
     */
    public CompositeHandler(Registration registration,
                            RequestResponseHandler requestResponseHandler,
                            SchemaRegistryClient schemaRegistryClient,
                            RegistrationCoordinator coordinator)
            throws RestClientException, IOException {
        this.registration = registration;
        this.isResourceRegistration = registration instanceof ResourceRegistration;
        this.requestResponseHandler = requestResponseHandler;
        this.coordinator = coordinator;
        this.schemas = new RegistrationSchemas(schemaRegistryClient, registration);
    }

    /**
     * Initializes all handlers managed by this composite handler.
     * <p>
     * For ResourceRegistration types, creates and initializes only an MCP resource
     * handler.
     * For other Registration types, creates and initializes both MCP and REST tool
     * handlers.
     *
     * @return A Mono that completes when all handlers are initialized
     */
    @Override
    public Mono<Void> initialize() {
        final List<Mono<Void>> ops = new ArrayList<>();

        if (registration instanceof ResourceRegistration resourceRegistration) {
            // For resource registrations, only register an MCP resource handler
            ops.add(registerHandler(
                    () -> new McpResourceHandler(resourceRegistration, schemas, requestResponseHandler,
                            coordinator.getMcpServer()),
                    "MCP resource handler"));
        } else {
            // For tool registrations, register both MCP and REST tool handlers
            ops.add(registerHandler(
                    () -> new McpToolHandler(registration, schemas, requestResponseHandler, coordinator.getMcpServer()),
                    "MCP tool handler"));

            ops.add(registerHandler(
                    () -> new RestToolHandler(registration, schemas, requestResponseHandler,
                            coordinator.getRestServer()),
                    "REST tool handler"));
        }

        return Mono.when(ops);
    }

    /**
     * Tears down all handlers managed by this composite handler.
     * <p>
     * This method gracefully handles errors during teardown of individual handlers,
     * ensuring that all handlers are attempted to be torn down even if some fail.
     *
     * @return A Mono that completes when all handlers are torn down
     */
    @Override
    public Mono<Void> teardown() {
        if (handlers.isEmpty()) {
            log.info("No handlers to tear down");
            return Mono.empty();
        }

        final List<Mono<Void>> unregisterOperations = handlers.stream()
                .map(handler -> handler.teardown()
                        .onErrorResume(error -> {
                            log.error("Error during handler teardown", error);
                            return Mono.empty(); // Continue with other teardowns even if this one fails
                        }))
                .toList();

        return Mono.when(unregisterOperations)
                .doOnSuccess(v -> log.info("Successfully torn down {} handlers", handlers.size()));
    }

    /**
     * Generic method to register a handler of any type.
     * <p>
     * This method creates the handler using the provided factory, initializes it,
     * and adds it to the list of managed handlers if initialization succeeds.
     * The method is designed to be resilient, preventing failures in one handler
     * from affecting other handler registrations.
     *
     * @param <T>            The type of server or context the handler is associated
     *                       with
     * @param handlerFactory A supplier that creates the handler instance
     * @param handlerType    A descriptive name of the handler type for logging
     *                       purposes
     * @return A Mono that completes when the handler is registered
     */
    private <T> Mono<Void> registerHandler(Supplier<RegistrationHandler<T>> handlerFactory, String handlerType) {
        RegistrationHandler<T> handler = handlerFactory.get();

        return handler.initialize()
                .doOnSuccess(v -> {
                    handlers.add(handler);
                    log.debug("Successfully registered {}", handlerType);
                })
                .doOnError(error -> log.error("Failed to register " + handlerType, error))
                .onErrorResume(error -> Mono.empty()); // Prevent a single handler failure from stopping other
        // registrations
    }
}
