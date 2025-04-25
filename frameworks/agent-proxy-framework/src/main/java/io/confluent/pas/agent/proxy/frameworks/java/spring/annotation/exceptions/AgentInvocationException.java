package io.confluent.pas.agent.proxy.frameworks.java.spring.annotation.exceptions;

/**
 * Exception thrown when there is an error during the invocation of the agent.
 */
public class AgentInvocationException extends RuntimeException {
    public AgentInvocationException(String message, Throwable cause) {
        super(message, cause);
    }
}