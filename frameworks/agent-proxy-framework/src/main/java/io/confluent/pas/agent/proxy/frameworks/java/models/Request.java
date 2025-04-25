package io.confluent.pas.agent.proxy.frameworks.java.models;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.confluent.kafka.schemaregistry.annotations.Schema;
import io.confluent.kafka.schemaregistry.json.JsonSchema;
import io.confluent.pas.agent.common.utils.JsonUtils;
import io.confluent.pas.agent.common.utils.SchemaUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

/**
 * Represents a request in the agent proxy framework.
 * This class encapsulates a request with an index and a payload.
 * The payload is represented as a JsonNode which allows for flexible data structures.
 * The class is annotated with a JSON schema that defines its structure.
 */
@Schema(value = """
        {
           "properties":{
              "requestIndex":{
                 "connect.index":0,
                 "type":"integer"
              },
              "payload":{
                 "connect.index":1,
                 "type":"object"
              }
           },
           "required":[
              "requestIndex",
              "payload"
           ],
           "title":"Record",
           "type":"object"
        }
        """, refs = {})
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Request {

    /**
     * The index of the request, used to identify and order requests.
     */
    private int requestIndex;

    /**
     * The payload of the request represented as a Map<String, Object>
     * This allows for flexible data structures in the payload.
     */
    private Map<String, Object> payload;

    /**
     * Generates a JsonSchema for the Request class with a specific payload class.
     * This method creates a schema that combines the Request schema with the payload's schema.
     *
     * @param payloadClass The class representing the payload, which should have a Schema annotation
     * @return A JsonSchema representing the Request with the specified payload schema
     * @throws JsonProcessingException If there is an error processing the JSON schema
     */
    public static JsonSchema getSchema(Class<?> payloadClass) throws JsonProcessingException {
        Schema payloadSchema = SchemaUtils.getSchemaAnnotation(payloadClass);
        Schema schema = SchemaUtils.getSchemaAnnotation(Request.class);

        JsonNode schemaNode = JsonUtils.toJsonNode(schema.value());
        JsonNode payloadNode = JsonUtils.toJsonNode(payloadSchema.value());

        // Set the property payload to the payload schema
        ObjectNode propertiesNode = (ObjectNode) schemaNode.get("properties");
        propertiesNode.set("payload", payloadNode);

        return new JsonSchema(schemaNode);
    }

    /**
     * Generates a JsonSchema for the Request class with an existing payload JsonSchema.
     * This method creates a schema that combines the Request schema with the provided payload schema.
     *
     * @param payloadSchema The JsonSchema representing the payload
     * @return A JsonSchema representing the Request with the specified payload schema
     * @throws JsonProcessingException If there is an error processing the JSON schema
     */
    public static JsonSchema getSchema(JsonSchema payloadSchema) throws JsonProcessingException {
        Schema schema = SchemaUtils.getSchemaAnnotation(Request.class);
        JsonNode schemaNode = JsonUtils.toJsonNode(schema.value());
        JsonNode payloadNode = payloadSchema.toJsonNode();

        // Set the property payload to the payload schema
        ObjectNode propertiesNode = (ObjectNode) schemaNode.get("properties");
        propertiesNode.set("payload", payloadNode);

        return new JsonSchema(schemaNode);
    }

}
