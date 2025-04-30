package io.confluent.pas.agent.proxy.rest.a2a.schemas;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.Objects;

/**
 * Represents the content of a file, either as base64 encoded bytes or a URI.
 * Ensures that either 'bytes' or 'uri' is provided, but not both.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "name",
        "mimeType",
        "bytes",
        "uri"
})
public class FileContent {

    @JsonProperty("name")
    private String name = null;

    @JsonProperty("mimeType")
    private String mimeType = null;

    @JsonProperty("bytes")
    private String bytes = null;

    @JsonProperty("uri")
    private String uri = null;

    public FileContent() {
    }

    public FileContent(String name, String mimeType, String bytes, String uri) {
        this.name = name;
        this.mimeType = mimeType;
        this.bytes = bytes;
        this.uri = uri;
    }

    @JsonProperty("name")
    public String getName() {
        return name;
    }

    @JsonProperty("name")
    public void setName(String name) {
        this.name = name;
    }

    @JsonProperty("mimeType")
    public String getMimeType() {
        return mimeType;
    }

    @JsonProperty("mimeType")
    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    @JsonProperty("bytes")
    public String getBytes() {
        return bytes;
    }

    @JsonProperty("bytes")
    public void setBytes(String bytes) {
        this.bytes = bytes;
    }

    @JsonProperty("uri")
    public String getUri() {
        return uri;
    }

    @JsonProperty("uri")
    public void setUri(String uri) {
        this.uri = uri;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        FileContent that = (FileContent) o;
        return Objects.equals(name, that.name) &&
                Objects.equals(mimeType, that.mimeType) &&
                Objects.equals(bytes, that.bytes) &&
                Objects.equals(uri, that.uri);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, mimeType, bytes, uri);
    }

    @Override
    public String toString() {
        return "FileContent{" +
                "name='" + name + '\'' +
                ", mimeType='" + mimeType + '\'' +
                ", bytes='" + bytes + '\'' +
                ", uri='" + uri + '\'' +
                '}';
    }
}