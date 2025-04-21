package io.confluent.pas.agent.a2a.schemas;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.Map;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "id",
        "sessionId",
        "message",
        "pushNotification",
        "historyLength",
        "metadata"
})
public class TaskSendParams {

    @JsonProperty("id")
    private String id;

    @JsonProperty("sessionId")
    private String sessionId;

    @JsonProperty("message")
    private Message message;

    @JsonProperty("pushNotification")
    private PushNotificationConfig pushNotification = null;

    @JsonProperty("historyLength")
    private Integer historyLength = null;

    @JsonProperty("metadata")
    private Map<String, Object> metadata = null;

    public TaskSendParams() {
    }

    public TaskSendParams(String id, String sessionId, Message message,
            PushNotificationConfig pushNotification, Integer historyLength,
            Map<String, Object> metadata) {
        this.id = id;
        this.sessionId = sessionId;
        this.message = message;
        this.pushNotification = pushNotification;
        this.historyLength = historyLength;
        this.metadata = metadata;
    }

    @JsonProperty("id")
    public String getId() {
        return id;
    }

    @JsonProperty("id")
    public void setId(String id) {
        this.id = id;
    }

    @JsonProperty("sessionId")
    public String getSessionId() {
        return sessionId;
    }

    @JsonProperty("sessionId")
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    @JsonProperty("message")
    public Message getMessage() {
        return message;
    }

    @JsonProperty("message")
    public void setMessage(Message message) {
        this.message = message;
    }

    @JsonProperty("pushNotification")
    public PushNotificationConfig getPushNotification() {
        return pushNotification;
    }

    @JsonProperty("pushNotification")
    public void setPushNotification(PushNotificationConfig pushNotification) {
        this.pushNotification = pushNotification;
    }

    @JsonProperty("historyLength")
    public Integer getHistoryLength() {
        return historyLength;
    }

    @JsonProperty("historyLength")
    public void setHistoryLength(Integer historyLength) {
        this.historyLength = historyLength;
    }

    @JsonProperty("metadata")
    public Map<String, Object> getMetadata() {
        return metadata;
    }

    @JsonProperty("metadata")
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        TaskSendParams that = (TaskSendParams) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(sessionId, that.sessionId) &&
                Objects.equals(message, that.message) &&
                Objects.equals(pushNotification, that.pushNotification) &&
                Objects.equals(historyLength, that.historyLength) &&
                Objects.equals(metadata, that.metadata);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, sessionId, message, pushNotification, historyLength, metadata);
    }

    @Override
    public String toString() {
        return "TaskSendParams{" +
                "id='" + id + '\'' +
                ", sessionId='" + sessionId + '\'' +
                ", message=" + message +
                ", pushNotification=" + pushNotification +
                ", historyLength=" + historyLength +
                ", metadata=" + metadata +
                '}';
    }
}