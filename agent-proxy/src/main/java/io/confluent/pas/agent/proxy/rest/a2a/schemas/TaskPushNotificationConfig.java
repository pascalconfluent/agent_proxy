package io.confluent.pas.agent.proxy.rest.a2a.schemas;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "id",
        "pushNotificationConfig"
})
public class TaskPushNotificationConfig {

    @JsonProperty("id")
    private String id;

    @JsonProperty("pushNotificationConfig")
    private PushNotificationConfig pushNotificationConfig;

    public TaskPushNotificationConfig() {
    }

    public TaskPushNotificationConfig(String id, PushNotificationConfig pushNotificationConfig) {
        this.id = id;
        this.pushNotificationConfig = pushNotificationConfig;
    }

    @JsonProperty("id")
    public String getId() {
        return id;
    }

    @JsonProperty("id")
    public void setId(String id) {
        this.id = id;
    }

    @JsonProperty("pushNotificationConfig")
    public PushNotificationConfig getPushNotificationConfig() {
        return pushNotificationConfig;
    }

    @JsonProperty("pushNotificationConfig")
    public void setPushNotificationConfig(PushNotificationConfig pushNotificationConfig) {
        this.pushNotificationConfig = pushNotificationConfig;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        TaskPushNotificationConfig that = (TaskPushNotificationConfig) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(pushNotificationConfig, that.pushNotificationConfig);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, pushNotificationConfig);
    }

    @Override
    public String toString() {
        return "TaskPushNotificationConfig{" +
                "id='" + id + '\'' +
                ", pushNotificationConfig=" + pushNotificationConfig +
                '}';
    }
}