package io.confluent.pas.agent.a2a.schemas;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "type",
        "text",
        "metadata"
})
public class TextPart implements Part {

    @JsonProperty("type")
    @JsonPropertyDescription("Type of the part")
    private final String type = "text";

    @JsonProperty("text")
    private String text;

    @JsonProperty("metadata")
    private Map<String, Object> metadata = null;

    public TextPart() {
    }

    public TextPart(String text) {
        this.text = text;
    }

    public TextPart(String text, Map<String, Object> metadata) {
        this.text = text;
        this.metadata = metadata;
    }

    @JsonProperty("type")
    @Override
    public String getType() {
        return type;
    }

    @JsonProperty("text")
    public String getText() {
        return text;
    }

    @JsonProperty("text")
    public void setText(String text) {
        this.text = text;
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
        TextPart textPart = (TextPart) o;
        return Objects.equals(type, textPart.type) &&
                Objects.equals(text, textPart.text) &&
                Objects.equals(metadata, textPart.metadata);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, text, metadata);
    }

    @Override
    public String toString() {
        return "TextPart{" +
                "type='" + type + '\'' +
                ", text='" + text + '\'' +
                ", metadata=" + metadata +
                '}';
    }
}