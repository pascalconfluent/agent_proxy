package io.confluent.pas.agent.a2a.schemas;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "id",
        "name",
        "description",
        "tags",
        "examples",
        "inputModes",
        "outputModes"
})
public class AgentSkill {

    @JsonProperty("id")
    private String id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("description")
    private String description = null;

    @JsonProperty("tags")
    private List<String> tags = null;

    @JsonProperty("examples")
    private List<String> examples = null;

    @JsonProperty("inputModes")
    private List<String> inputModes = null;

    @JsonProperty("outputModes")
    private List<String> outputModes = null;

    public AgentSkill() {
    }

    public AgentSkill(String id, String name, String description, List<String> tags,
            List<String> examples, List<String> inputModes, List<String> outputModes) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.tags = tags;
        this.examples = examples;
        this.inputModes = inputModes;
        this.outputModes = outputModes;
    }

    @JsonProperty("id")
    public String getId() {
        return id;
    }

    @JsonProperty("id")
    public void setId(String id) {
        this.id = id;
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

    @JsonProperty("tags")
    public List<String> getTags() {
        return tags;
    }

    @JsonProperty("tags")
    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    @JsonProperty("examples")
    public List<String> getExamples() {
        return examples;
    }

    @JsonProperty("examples")
    public void setExamples(List<String> examples) {
        this.examples = examples;
    }

    @JsonProperty("inputModes")
    public List<String> getInputModes() {
        return inputModes;
    }

    @JsonProperty("inputModes")
    public void setInputModes(List<String> inputModes) {
        this.inputModes = inputModes;
    }

    @JsonProperty("outputModes")
    public List<String> getOutputModes() {
        return outputModes;
    }

    @JsonProperty("outputModes")
    public void setOutputModes(List<String> outputModes) {
        this.outputModes = outputModes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        AgentSkill that = (AgentSkill) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(name, that.name) &&
                Objects.equals(description, that.description) &&
                Objects.equals(tags, that.tags) &&
                Objects.equals(examples, that.examples) &&
                Objects.equals(inputModes, that.inputModes) &&
                Objects.equals(outputModes, that.outputModes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, description, tags, examples, inputModes, outputModes);
    }

    @Override
    public String toString() {
        return "AgentSkill{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", tags=" + tags +
                ", examples=" + examples +
                ", inputModes=" + inputModes +
                ", outputModes=" + outputModes +
                '}';
    }
}