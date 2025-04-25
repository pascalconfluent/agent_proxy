package io.confluent.pas.agent.proxy.registration.handlers;

import io.confluent.pas.agent.common.services.schemas.Registration;
import io.confluent.pas.agent.proxy.registration.schemas.RegistrationSchemas;
import reactor.core.publisher.Mono;

/**
 * Interface for handling registration of tools and resources.
 */
public interface RegistrationHandler<SRV> {

    /**
     * Gets the registration for the tool or resource.
     *
     * @return the registration
     */
    Registration getRegistration();

    /**
     * Gets the registration schemas for the tool or resource.
     *
     * @return the registration schemas
     */
    RegistrationSchemas getSchemas();
    
    /**
     * Initializes the tool or resource with the specified server.
     *
     * @return a Mono that completes when the registration is complete
     */
    Mono<Void> initialize();

    /**
     * Teardown the tool or resource with the specified server.
     *
     * @return a Mono that completes when the un-registration is complete
     */
    Mono<Void> teardown();
}