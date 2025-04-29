package io.confluent.pas.agent.proxy.rest.a2a.schemas;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "id",
        "sessionId",
        "status",
        "artifacts",
        "history",
        "metadata"
})
public class Task {

    @JsonProperty("id")
    private String id;

    @JsonProperty("sessionId")
    private String sessionId = null;

    @JsonProperty("status")
    private TaskStatus status;

    @JsonProperty("artifacts")
    private List<Artifact> artifacts = null;

    @JsonProperty("history")
    private List<Message> history = null;

    @JsonProperty("metadata")
    private Map<String, Object> metadata = null;

    public Task() {
    }

    public Task(String id, String sessionId, TaskStatus status, List<Artifact> artifacts,
                List<Message> history, Map<String, Object> metadata) {
        this.id = id;
        this.sessionId = sessionId;
        this.status = status;
        this.artifacts = artifacts;
        this.history = history;
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

    @JsonProperty("status")
    public TaskStatus getStatus() {
        return status;
    }

    @JsonProperty("status")
    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    @JsonProperty("artifacts")
    public List<Artifact> getArtifacts() {
        return artifacts;
    }

    @JsonProperty("artifacts")
    public void setArtifacts(List<Artifact> artifacts) {
        this.artifacts = artifacts;
    }

    @JsonProperty("history")
    public List<Message> getHistory() {
        return history;
    }

    @JsonProperty("history")
    public void setHistory(List<Message> history) {
        this.history = history;
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
        Task task = (Task) o;
        return Objects.equals(id, task.id) &&
                Objects.equals(sessionId, task.sessionId) &&
                Objects.equals(status, task.status) &&
                Objects.equals(artifacts, task.artifacts) &&
                Objects.equals(history, task.history) &&
                Objects.equals(metadata, task.metadata);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, sessionId, status, artifacts, history, metadata);
    }

    @Override
    public String toString() {
        return "Task{" +
                "id='" + id + '\'' +
                ", sessionId='" + sessionId + '\'' +
                ", status=" + status +
                ", artifacts=" + artifacts +
                ", history=" + history +
                ", metadata=" + metadata +
                '}';
    }
}