package io.confluent.pas.agent.a2a.schemas;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.time.LocalDateTime;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "state",
        "message",
        "timestamp"
})
public class TaskStatus {

    @JsonProperty("state")
    private TaskState state;

    @JsonProperty("message")
    private Message message = null;

    @JsonProperty("timestamp")
    private LocalDateTime timestamp;

    public TaskStatus() {
    }

    public TaskStatus(TaskState state, Message message, LocalDateTime timestamp) {
        this.state = state;
        this.message = message;
        this.timestamp = timestamp;
    }

    @JsonProperty("state")
    public TaskState getState() {
        return state;
    }

    @JsonProperty("state")
    public void setState(TaskState state) {
        this.state = state;
    }

    @JsonProperty("message")
    public Message getMessage() {
        return message;
    }

    @JsonProperty("message")
    public void setMessage(Message message) {
        this.message = message;
    }

    @JsonProperty("timestamp")
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    @JsonProperty("timestamp")
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        TaskStatus that = (TaskStatus) o;
        return state == that.state &&
                Objects.equals(message, that.message) &&
                Objects.equals(timestamp, that.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(state, message, timestamp);
    }

    @Override
    public String toString() {
        return "TaskStatus{" +
                "state=" + state +
                ", message=" + message +
                ", timestamp=" + timestamp +
                '}';
    }
}