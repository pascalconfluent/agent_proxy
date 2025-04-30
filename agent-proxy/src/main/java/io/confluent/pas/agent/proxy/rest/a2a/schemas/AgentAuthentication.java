package io.confluent.pas.agent.proxy.rest.a2a.schemas;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "schemes",
        "credentials"
})
public class AgentAuthentication {

    @JsonProperty("schemes")
    private List<String> schemes;

    @JsonProperty("credentials")
    private String credentials = null;

    public AgentAuthentication() {
    }

    public AgentAuthentication(List<String> schemes, String credentials) {
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

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        AgentAuthentication that = (AgentAuthentication) o;
        return Objects.equals(schemes, that.schemes) &&
                Objects.equals(credentials, that.credentials);
    }

    @Override
    public int hashCode() {
        return Objects.hash(schemes, credentials);
    }

    @Override
    public String toString() {
        return "AgentAuthentication{" +
                "schemes=" + schemes +
                ", credentials='" + credentials + '\'' +
                '}';
    }
}