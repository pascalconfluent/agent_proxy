package io.confluent.pas.agent.a2a.schemas;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "streaming",
        "pushNotifications",
        "stateTransitionHistory"
})
public class AgentCapabilities {

    @JsonProperty("streaming")
    private boolean streaming = false;

    @JsonProperty("pushNotifications")
    private boolean pushNotifications = false;

    @JsonProperty("stateTransitionHistory")
    private boolean stateTransitionHistory = false;

    public AgentCapabilities() {
    }

    public AgentCapabilities(boolean streaming, boolean pushNotifications, boolean stateTransitionHistory) {
        this.streaming = streaming;
        this.pushNotifications = pushNotifications;
        this.stateTransitionHistory = stateTransitionHistory;
    }

    @JsonProperty("streaming")
    public boolean isStreaming() {
        return streaming;
    }

    @JsonProperty("streaming")
    public void setStreaming(boolean streaming) {
        this.streaming = streaming;
    }

    @JsonProperty("pushNotifications")
    public boolean isPushNotifications() {
        return pushNotifications;
    }

    @JsonProperty("pushNotifications")
    public void setPushNotifications(boolean pushNotifications) {
        this.pushNotifications = pushNotifications;
    }

    @JsonProperty("stateTransitionHistory")
    public boolean isStateTransitionHistory() {
        return stateTransitionHistory;
    }

    @JsonProperty("stateTransitionHistory")
    public void setStateTransitionHistory(boolean stateTransitionHistory) {
        this.stateTransitionHistory = stateTransitionHistory;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        AgentCapabilities that = (AgentCapabilities) o;
        return streaming == that.streaming &&
                pushNotifications == that.pushNotifications &&
                stateTransitionHistory == that.stateTransitionHistory;
    }

    @Override
    public int hashCode() {
        return Objects.hash(streaming, pushNotifications, stateTransitionHistory);
    }

    @Override
    public String toString() {
        return "AgentCapabilities{" +
                "streaming=" + streaming +
                ", pushNotifications=" + pushNotifications +
                ", stateTransitionHistory=" + stateTransitionHistory +
                '}';
    }
}