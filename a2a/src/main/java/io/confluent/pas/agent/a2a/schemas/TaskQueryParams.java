package io.confluent.pas.agent.a2a.schemas;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.Map;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "id",
        "historyLength",
        "metadata"
})
public class TaskQueryParams {

    @JsonProperty("id")
    private String id;

    @JsonProperty("historyLength")
    private Integer historyLength = null;

    @JsonProperty("metadata")
    private Map<String, Object> metadata = null;

    public TaskQueryParams() {
    }

    public TaskQueryParams(String id, Integer historyLength, Map<String, Object> metadata) {
        this.id = id;
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
        TaskQueryParams that = (TaskQueryParams) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(historyLength, that.historyLength) &&
                Objects.equals(metadata, that.metadata);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, historyLength, metadata);
    }

    @Override
    public String toString() {
        return "TaskQueryParams{" +
                "id='" + id + '\'' +
                ", historyLength=" + historyLength +
                ", metadata=" + metadata +
                '}';
    }
}