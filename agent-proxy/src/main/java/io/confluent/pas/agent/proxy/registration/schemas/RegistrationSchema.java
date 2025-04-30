package io.confluent.pas.agent.proxy.registration.schemas;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import io.confluent.kafka.schemaregistry.json.JsonSchema;
import io.confluent.kafka.schemaregistry.json.JsonSchemaUtils;
import io.confluent.pas.agent.common.utils.JsonUtils;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * The registration schema
 */
@Getter
@NoArgsConstructor
public class RegistrationSchema {
    private String schema;
    private String payloadSchema;
    private JsonSchema jsonSchema;

    public RegistrationSchema(String schema) throws JsonProcessingException {
        JsonNode schemaNode = JsonUtils.toJsonNode(schema);
        if (schemaNode.has("properties") && schemaNode.get("properties").has("payload")) {
            this.payloadSchema = schemaNode.get("properties").get("payload").toString();
        } else {
            this.payloadSchema = schema;
        }

        this.schema = schema;
        this.jsonSchema = new JsonSchema(schema);
    }

    /**
     * Envelope the payload
     *
     * @param payload The payload
     * @return The enveloped payload
     */
    public JsonNode envelope(Object payload) {
        final JsonNode jsonNode = JsonUtils.toJsonNode(payload);
        return JsonSchemaUtils.envelope(jsonSchema, jsonNode);
    }
}
