package io.confluent.pas.agent.a2a.schemas;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "schemes",
        "credentials"
})
public class AuthenticationInfo {

    @JsonProperty("schemes")
    private List<String> schemes;

    @JsonProperty("credentials")
    private String credentials = null;

    private Map<String, Object> additionalProperties = new HashMap<>();

    public AuthenticationInfo() {
    }

    public AuthenticationInfo(List<String> schemes, String credentials) {
        this.schemes = schemes;
        this.credentials = credentials;
    }

    @JsonProperty("schemes")
    public List<String> getSchemes() {
        return schemes;
    }

    @JsonProperty("schemes")
    public void setSchemes(List<String> schemes) {
        this.schemes = schemes;
    }

    @JsonProperty("credentials")
    public String getCredentials() {
        return credentials;
    }

    @JsonProperty("credentials")
    public void setCredentials(String credentials) {
        this.credentials = credentials;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        AuthenticationInfo that = (AuthenticationInfo) o;
        return Objects.equals(schemes, that.schemes) &&
                Objects.equals(credentials, that.credentials) &&
                Objects.equals(additionalProperties, that.additionalProperties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(schemes, credentials, additionalProperties);
    }

    @Override
    public String toString() {
        return "AuthenticationInfo{" +
                "schemes=" + schemes +
                ", credentials='" + credentials + '\'' +
                ", additionalProperties=" + additionalProperties +
                '}';
    }
}