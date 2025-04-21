package io.confluent.pas.agent.proxy.registration.events;

import io.confluent.pas.agent.common.services.schemas.Registration;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Event for deleted registration.
 */
@Getter
public class DeletedRegistrationEvent extends ApplicationEvent {

    private final Registration registration;

    public DeletedRegistrationEvent(Object source, Registration registration) {
        super(source);
        this.registration = registration;
    }

}
