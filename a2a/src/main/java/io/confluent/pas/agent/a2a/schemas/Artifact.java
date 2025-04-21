package io.confluent.pas.agent.a2a.schemas;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "name",
        "description",
        "parts",
        "index",
        "append",
        "lastChunk",
        "metadata"
})
public class Artifact {

    @JsonProperty("name")
    private String name = null;

    @JsonProperty("description")
    private String description = null;

    @JsonProperty("parts")
    private List<Part> parts;

    @JsonProperty("index")
    private Integer index = 0;

    @JsonProperty("append")
    private Boolean append = null;

    @JsonProperty("lastChunk")
    private Boolean lastChunk = null;

    @JsonProperty("metadata")
    private Map<String, Object> metadata = null;

    public Artifact() {
    }

    public Artifact(String name, String description, List<Part> parts, Integer index,
            Boolean append, Boolean lastChunk, Map<String, Object> metadata) {
        this.name = name;
        this.description = description;
        this.parts = parts;
        this.index = index;
        this.append = append;
        this.lastChunk = lastChunk;
        this.metadata = metadata;
    }

    @JsonProperty("name")
    public String getName() {
        return name;
    }

    @JsonProperty("name")
    public void setName(String name) {
        this.name = name;
    }

    @JsonProperty("description")
    public String getDescription() {
        return description;
    }

    @JsonProperty("description")
    public void setDescription(String description) {
        this.description = description;
    }

    @JsonProperty("parts")
    public List<Part> getParts() {
        return parts;
    }

    @JsonProperty("parts")
    public void setParts(List<Part> parts) {
        this.parts = parts;
    }

    @JsonProperty("index")
    public Integer getIndex() {
        return index;
    }

    @JsonProperty("index")
    public void setIndex(Integer index) {
        this.index = index;
    }

    @JsonProperty("append")
    public Boolean getAppend() {
        return append;
    }

    @JsonProperty("append")
    public void setAppend(Boolean append) {
        this.append = append;
    }

    @JsonProperty("lastChunk")
    public Boolean getLastChunk() {
        return lastChunk;
    }

    @JsonProperty("lastChunk")
    public void setLastChunk(Boolean lastChunk) {
        this.lastChunk = lastChunk;
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
        Artifact artifact = (Artifact) o;
        return Objects.equals(name, artifact.name) &&
                Objects.equals(description, artifact.description) &&
                Objects.equals(parts, artifact.parts) &&
                Objects.equals(index, artifact.index) &&
                Objects.equals(append, artifact.append) &&
                Objects.equals(lastChunk, artifact.lastChunk) &&
                Objects.equals(metadata, artifact.metadata);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, description, parts, index, append, lastChunk, metadata);
    }

    @Override
    public String toString() {
        return "Artifact{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", parts=" + parts +
                ", index=" + index +
                ", append=" + append +
                ", lastChunk=" + lastChunk +
                ", metadata=" + metadata +
                '}';
    }
}