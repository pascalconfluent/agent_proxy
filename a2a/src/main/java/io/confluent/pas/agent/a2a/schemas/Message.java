package io.confluent.pas.agent.a2a.schemas;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "role",
        "parts",
        "metadata"
})
public class Message {

    @JsonProperty("role")
    private String role;

    @JsonProperty("parts")
    private List<Part> parts;

    @JsonProperty("metadata")
    private Map<String, Object> metadata = null;

    public Message() {
    }

    public Message(String role, List<Part> parts) {
        this.role = role;
        this.parts = parts;
    }

    public Message(String role, List<Part> parts, Map<String, Object> metadata) {
        this.role = role;
        this.parts = parts;
        this.metadata = metadata;
    }

    @JsonProperty("role")
    public String getRole() {
        return role;
    }

    @JsonProperty("role")
    public void setRole(String role) {
        this.role = role;
    }

    @JsonProperty("parts")
    public List<Part> getParts() {
        return parts;
    }

    @JsonProperty("parts")
    public void setParts(List<Part> parts) {
        this.parts = parts;
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
        Message message = (Message) o;
        return Objects.equals(role, message.role) &&
                Objects.equals(parts, message.parts) &&
                Objects.equals(metadata, message.metadata);
    }

    @Override
    public int hashCode() {
        return Objects.hash(role, parts, metadata);
    }

    @Override
    public String toString() {
        return "Message{" +
                "role='" + role + '\'' +
                ", parts=" + parts +
                ", metadata=" + metadata +
                '}';
    }
}