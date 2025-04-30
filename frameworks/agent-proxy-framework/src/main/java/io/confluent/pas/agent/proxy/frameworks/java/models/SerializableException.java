package io.confluent.pas.agent.proxy.frameworks.java.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SerializableException implements Serializable {
    // Getters
    private String type;
    private String message;
    private StackTraceElement[] stackTrace;

    public SerializableException(Exception e) {
        this.type = e.getClass().getName();
        this.message = e.getMessage();
        this.stackTrace = e.getStackTrace();
    }

    public SerializableException(Throwable e) {
        this.type = e.getClass().getName();
        this.message = e.getMessage();
        this.stackTrace = e.getStackTrace();
    }

    public Throwable toThrowable() {
        Throwable exception;
        try {
            Class<?> clazz = Class.forName(type);
            exception = (Exception) clazz.getConstructor(String.class).newInstance(message);
            exception.setStackTrace(stackTrace);
        } catch (Exception e) {
            exception = new Throwable("Failed to deserialize exception", e);
        }

        return exception;
    }
}
