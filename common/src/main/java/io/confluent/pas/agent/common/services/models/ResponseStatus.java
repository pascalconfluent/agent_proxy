package io.confluent.pas.agent.common.services.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ResponseStatus {
    INPUT_REQUIRED,
    COMPLETED,
    FAILED,
    ERROR;

    @JsonValue
    public String getValue() {
        return this.name().toLowerCase();
    }

    @JsonCreator
    public static ResponseStatus fromString(String value) {
        return value != null ? valueOf(value.toUpperCase()) : null;
    }

}
