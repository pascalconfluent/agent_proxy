package io.confluent.pas.agent.proxy.rest.a2a.schemas;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "url",
        "token",
        "authentication"
})
public class PushNotificationConfig {

    @JsonProperty("url")
    private String url;

    @JsonProperty("token")
    private String token;

    @JsonProperty("authentication")
    private AuthenticationInfo authentication = null;

    public PushNotificationConfig() {
    }

    public PushNotificationConfig(String url, String token, AuthenticationInfo authentication) {
        this.url = url;
        this.token = token;
        this.authentication = authentication;
    }

    @JsonProperty("url")
    public String getUrl() {
        return url;
    }

    @JsonProperty("url")
    public void setUrl(String url) {
        this.url = url;
    }

    @JsonProperty("token")
    public String getToken() {
        return token;
    }

    @JsonProperty("token")
    public void setToken(String token) {
        this.token = token;
    }

    @JsonProperty("authentication")
    public AuthenticationInfo getAuthentication() {
        return authentication;
    }

    @JsonProperty("authentication")
    public void setAuthentication(AuthenticationInfo authentication) {
        this.authentication = authentication;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        PushNotificationConfig that = (PushNotificationConfig) o;
        return Objects.equals(url, that.url) &&
                Objects.equals(token, that.token) &&
                Objects.equals(authentication, that.authentication);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url, token, authentication);
    }

    @Override
    public String toString() {
        return "PushNotificationConfig{" +
                "url='" + url + '\'' +
                ", token='" + token + '\'' +
                ", authentication=" + authentication +
                '}';
    }
}