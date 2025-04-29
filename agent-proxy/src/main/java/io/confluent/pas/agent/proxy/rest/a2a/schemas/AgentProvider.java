package io.confluent.pas.agent.proxy.rest.a2a.schemas;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "organization",
        "url"
})
public class AgentProvider {

    @JsonProperty("organization")
    private String organization;

    @JsonProperty("url")
    private String url = null;

    public AgentProvider() {
    }

    public AgentProvider(String organization, String url) {
        this.organization = organization;
        this.url = url;
    }

    @JsonProperty("organization")
    public String getOrganization() {
        return organization;
    }

    @JsonProperty("organization")
    public void setOrganization(String organization) {
        this.organization = organization;
    }

    @JsonProperty("url")
    public String getUrl() {
        return url;
    }

    @JsonProperty("url")
    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        AgentProvider that = (AgentProvider) o;
        return Objects.equals(organization, that.organization) &&
                Objects.equals(url, that.url);
    }

    @Override
    public int hashCode() {
        return Objects.hash(organization, url);
    }

    @Override
    public String toString() {
        return "AgentProvider{" +
                "organization='" + organization + '\'' +
                ", url='" + url + '\'' +
                '}';
    }
}