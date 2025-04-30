package io.confluent.pas.agent.proxy.frameworks.java.spring.annotation.exceptions;

/**
 * Exception thrown when there is an error during the initialization of the agent.
 */
public class AgentInitializationException extends RuntimeException {
    public AgentInitializationException(String message, Throwable cause) {
        super(message, cause);
    }

    public AgentInitializationException(String message) {
        super(message);
    }
}
