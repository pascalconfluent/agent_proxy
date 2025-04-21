package io.confluent.pas.agent.a2a.schemas;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.Map;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "id",
        "metadata"
})
public class TaskIdParams {

    @JsonProperty("id")
    private String id;

    @JsonProperty("metadata")
    private Map<String, Object> metadata = null;

    public TaskIdParams() {
    }

    public TaskIdParams(String id, Map<String, Object> metadata) {
        this.id = id;
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
        TaskIdParams that = (TaskIdParams) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(metadata, that.metadata);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, metadata);
    }

    @Override
    public String toString() {
        return "TaskIdParams{" +
                "id='" + id + '\'' +
                ", metadata=" + metadata +
                '}';
    }
}