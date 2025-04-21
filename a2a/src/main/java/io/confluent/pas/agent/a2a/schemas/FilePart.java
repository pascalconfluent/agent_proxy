package io.confluent.pas.agent.a2a.schemas;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.Map;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "type",
        "file",
        "metadata"
})
public class FilePart implements Part {

    @JsonProperty("type")
    @JsonPropertyDescription("Type of the part")
    private final String type = "file";

    @JsonProperty("file")
    private FileContent file;

    @JsonProperty("metadata")
    private Map<String, Object> metadata = null;

    public FilePart() {
    }

    public FilePart(FileContent file) {
        this.file = file;
    }

    public FilePart(FileContent file, Map<String, Object> metadata) {
        this.file = file;
        this.metadata = metadata;
    }

    @JsonProperty("type")
    @Override
    public String getType() {
        return type;
    }

    @JsonProperty("file")
    public FileContent getFile() {
        return file;
    }

    @JsonProperty("file")
    public void setFile(FileContent file) {
        this.file = file;
    }

    @JsonProperty("metadata")
    @Override
    public Map<String, Object> getMetadata() {
        return metadata;
    }

    @JsonProperty("metadata")
    @Override
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        FilePart filePart = (FilePart) o;
        return Objects.equals(type, filePart.type) &&
                Objects.equals(file, filePart.file) &&
                Objects.equals(metadata, filePart.metadata);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, file, metadata);
    }

    @Override
    public String toString() {
        return "FilePart{" +
                "type='" + type + '\'' +
                ", file=" + file +
                ", metadata=" + metadata +
                '}';
    }
}