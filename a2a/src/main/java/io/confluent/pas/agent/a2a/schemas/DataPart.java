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
        "data",
        "metadata"
})
public class DataPart implements Part {

    @JsonProperty("type")
    @JsonPropertyDescription("Type of the part")
    private final String type = "data";

    @JsonProperty("data")
    private Map<String, Object> data;

    @JsonProperty("metadata")
    private Map<String, Object> metadata = null;

    public DataPart() {
    }

    public DataPart(Map<String, Object> data) {
        this.data = data;
    }

    public DataPart(Map<String, Object> data, Map<String, Object> metadata) {
        this.data = data;
        this.metadata = metadata;
    }

    @JsonProperty("type")
    @Override
    public String getType() {
        return type;
    }

    @JsonProperty("data")
    public Map<String, Object> getData() {
        return data;
    }

    @JsonProperty("data")
    public void setData(Map<String, Object> data) {
        this.data = data;
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
        DataPart dataPart = (DataPart) o;
        return Objects.equals(type, dataPart.type) &&
                Objects.equals(data, dataPart.data) &&
                Objects.equals(metadata, dataPart.metadata);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, data, metadata);
    }

    @Override
    public String toString() {
        return "DataPart{" +
                "type='" + type + '\'' +
                ", data=" + data +
                ", metadata=" + metadata +
                '}';
    }
}