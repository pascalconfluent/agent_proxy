package io.confluent.pas.agent.a2a.schemas;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "name",
        "description",
        "url",
        "provider",
        "version",
        "documentationUrl",
        "capabilities",
        "authentication",
        "defaultInputModes",
        "defaultOutputModes",
        "skills"
})
public class AgentCard {

    @JsonProperty("name")
    private String name;

    @JsonProperty("description")
    private String description = null;

    @JsonProperty("url")
    private String url;

    @JsonProperty("provider")
    private AgentProvider provider = null;

    @JsonProperty("version")
    private String version;

    @JsonProperty("documentationUrl")
    private String documentationUrl = null;

    @JsonProperty("capabilities")
    private AgentCapabilities capabilities;

    @JsonProperty("authentication")
    private AgentAuthentication authentication = null;

    @JsonProperty("defaultInputModes")
    private List<String> defaultInputModes = List.of("text");

    @JsonProperty("defaultOutputModes")
    private List<String> defaultOutputModes = List.of("text");

    @JsonProperty("skills")
    private List<AgentSkill> skills;

    public AgentCard() {
    }

    public AgentCard(String name, String description, String url, AgentProvider provider,
            String version, String documentationUrl, AgentCapabilities capabilities,
            AgentAuthentication authentication, List<String> defaultInputModes,
            List<String> defaultOutputModes, List<AgentSkill> skills) {
        this.name = name;
        this.description = description;
        this.url = url;
        this.provider = provider;
        this.version = version;
        this.documentationUrl = documentationUrl;
        this.capabilities = capabilities;
        this.authentication = authentication;
        this.defaultInputModes = defaultInputModes;
        this.defaultOutputModes = defaultOutputModes;
        this.skills = skills;
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

    @JsonProperty("url")
    public String getUrl() {
        return url;
    }

    @JsonProperty("url")
    public void setUrl(String url) {
        this.url = url;
    }

    @JsonProperty("provider")
    public AgentProvider getProvider() {
        return provider;
    }

    @JsonProperty("provider")
    public void setProvider(AgentProvider provider) {
        this.provider = provider;
    }

    @JsonProperty("version")
    public String getVersion() {
        return version;
    }

    @JsonProperty("version")
    public void setVersion(String version) {
        this.version = version;
    }

    @JsonProperty("documentationUrl")
    public String getDocumentationUrl() {
        return documentationUrl;
    }

    @JsonProperty("documentationUrl")
    public void setDocumentationUrl(String documentationUrl) {
        this.documentationUrl = documentationUrl;
    }

    @JsonProperty("capabilities")
    public AgentCapabilities getCapabilities() {
        return capabilities;
    }

    @JsonProperty("capabilities")
    public void setCapabilities(AgentCapabilities capabilities) {
        this.capabilities = capabilities;
    }

    @JsonProperty("authentication")
    public AgentAuthentication getAuthentication() {
        return authentication;
    }

    @JsonProperty("authentication")
    public void setAuthentication(AgentAuthentication authentication) {
        this.authentication = authentication;
    }

    @JsonProperty("defaultInputModes")
    public List<String> getDefaultInputModes() {
        return defaultInputModes;
    }

    @JsonProperty("defaultInputModes")
    public void setDefaultInputModes(List<String> defaultInputModes) {
        this.defaultInputModes = defaultInputModes;
    }

    @JsonProperty("defaultOutputModes")
    public List<String> getDefaultOutputModes() {
        return defaultOutputModes;
    }

    @JsonProperty("defaultOutputModes")
    public void setDefaultOutputModes(List<String> defaultOutputModes) {
        this.defaultOutputModes = defaultOutputModes;
    }

    @JsonProperty("skills")
    public List<AgentSkill> getSkills() {
        return skills;
    }

    @JsonProperty("skills")
    public void setSkills(List<AgentSkill> skills) {
        this.skills = skills;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        AgentCard agentCard = (AgentCard) o;
        return Objects.equals(name, agentCard.name) &&
                Objects.equals(description, agentCard.description) &&
                Objects.equals(url, agentCard.url) &&
                Objects.equals(provider, agentCard.provider) &&
                Objects.equals(version, agentCard.version) &&
                Objects.equals(documentationUrl, agentCard.documentationUrl) &&
                Objects.equals(capabilities, agentCard.capabilities) &&
                Objects.equals(authentication, agentCard.authentication) &&
                Objects.equals(defaultInputModes, agentCard.defaultInputModes) &&
                Objects.equals(defaultOutputModes, agentCard.defaultOutputModes) &&
                Objects.equals(skills, agentCard.skills);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, description, url, provider, version, documentationUrl,
                capabilities, authentication, defaultInputModes, defaultOutputModes, skills);
    }

    @Override
    public String toString() {
        return "AgentCard{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", url='" + url + '\'' +
                ", provider=" + provider +
                ", version='" + version + '\'' +
                ", documentationUrl='" + documentationUrl + '\'' +
                ", capabilities=" + capabilities +
                ", authentication=" + authentication +
                ", defaultInputModes=" + defaultInputModes +
                ", defaultOutputModes=" + defaultOutputModes +
                ", skills=" + skills +
                '}';
    }
}