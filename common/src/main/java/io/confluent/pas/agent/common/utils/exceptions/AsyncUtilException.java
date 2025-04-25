package io.confluent.pas.agent.common.utils.exceptions;

public class AsyncUtilException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public AsyncUtilException(String message) {
        super(message);
    }

    public AsyncUtilException(String message, Throwable cause) {
        super(message, cause);
    }

    public AsyncUtilException(Throwable cause) {
        super(cause);
    }
}
