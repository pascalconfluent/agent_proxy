package io.confluent.pas.agent.common.services.models;

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

import java.io.IOException;
import java.util.Map;

/**
 * Response model for agent proxy framework communications.
 * This class represents the subscriptionResponse structure sent back from a service after processing a request.
 * The schema is defined using annotations for automatic schema generation and validation.
 */
@Schema(value = """
        {
           "properties":{
              "requestIndex":{
                 "connect.index":0,
                 "type":"integer"
              },
              "status":{
                 "connect.index":1,
                 "type":"string",
                 "enum":["INPUT_REQUIRED", "COMPLETED", "FAILED", "ERROR"]
              },
              "message":{
                 "connect.index":2,
                 "type":"string"
              },
              "exception":{
                 "connect.index":3,
                 "type":"object",
                 "properties":{
                    "className":{
                       "type":"string"
                    },
                    "message":{
                       "type":"string"
                    },
                    "stackTrace":{
                       "type":"array",
                       "items":{
                          "type":"string"
                       }
                    }
                 }
              },
              "payload":{
                 "connect.index":4,
                 "type":"object"
              }
           },
           "required":[
              "requestIndex",
              "status"
           ],
           "title":"Response",
           "type":"object"
        }
        """, refs = {})
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Response {

    /**
     * Unique identifier for the request this subscriptionResponse corresponds to.
     * A value of -1 indicates an error subscriptionResponse not associated with a specific request.
     */
    private int requestIndex;

    /**
     * Status of the subscriptionResponse (SUCCESS or ERROR).
     */
    private ResponseStatus status;

    /**
     * Optional message providing additional context about the subscriptionResponse.
     * For error responses, this typically contains error details.
     */
    private String message;

    /**
     * Exception information if an error occurred during processing.
     * This field provides structured exception details for error diagnosis.
     */
    private SerializableException exception;

    /**
     * The actual payload of the subscriptionResponse containing the requested data.
     * This is a flexible Map<String, Object>.
     */
    private Map<String, Object> payload;

    /**
     * Constructor for creating an error subscriptionResponse with a custom message and
     * exception.
     *
     * @param message   Error message describing what went wrong
     * @param exception Serialized exception for detailed error information
     */
    public Response(String message, SerializableException exception) {
        this.requestIndex = -1;
        this.status = ResponseStatus.ERROR;
        this.message = message;
        this.exception = exception;
    }

    /**
     * Constructor for creating an error subscriptionResponse from a standard Exception.
     * Automatically converts the exception to a SerializableException.
     *
     * @param exception The Exception that occurred
     */
    public Response(Exception exception) {
        this.requestIndex = -1;
        this.status = ResponseStatus.ERROR;
        this.message = exception.getMessage();
        this.exception = new SerializableException(exception);
    }

    /**
     * Constructor for creating an error subscriptionResponse from any Throwable.
     * Automatically converts the Throwable to a SerializableException.
     *
     * @param exception The Throwable that occurred
     */
    public Response(Throwable exception) {
        this.requestIndex = -1;
        this.status = ResponseStatus.ERROR;
        this.message = exception.getMessage();
        this.exception = new SerializableException(exception);
    }

    /**
     * Generates a JSON Schema for a Response with a specific payload type.
     * This method combines the Response schema with the schema of the payload class
     * to create a complete schema definition for validation and serialization.
     *
     * @param payloadClass The class type of the payload, which must be annotated with @Schema
     * @return A JsonSchema object representing the combined schema
     * @throws JsonProcessingException  If there's an error processing the JSON schema
     * @throws IllegalArgumentException If the payload class is not properly annotated
     */
    public static JsonSchema getSchema(Class<?> payloadClass) throws IOException {
        // Extract schema annotations from the payload class and Response class
        String payloadSchema = SchemaUtils.getSchema(payloadClass);
        String schema = SchemaUtils.getSchema(Response.class);

        // Convert both schemas to JsonNode objects for manipulation
        JsonNode schemaNode = JsonUtils.toJsonNode(schema);
        JsonNode payloadNode = JsonUtils.toJsonNode(payloadSchema);

        // Set the property payload to the payload schema
        ObjectNode propertiesNode = (ObjectNode) schemaNode.get("properties");
        propertiesNode.set("payload", payloadNode);

        return new JsonSchema(schemaNode);
    }

    /**
     * Generates a JSON Schema for a Response with a specific payload schema.
     * This method combines the Response schema with the provided payload schema
     * to create a complete schema definition for validation and serialization.
     * This overload is useful when you already have a JsonSchema object for the
     * payload.
     *
     * @param payloadSchema The JsonSchema representing the payload structure
     * @return A JsonSchema object representing the combined schema
     * @throws JsonProcessingException If there's an error processing the JSON schema
     */
    public static JsonSchema getSchema(JsonSchema payloadSchema) throws IOException {
        // Extract schema annotation from the Response class
        String schema = SchemaUtils.getSchema(Response.class);

        // Convert both schemas to JsonNode objects for manipulation
        JsonNode schemaNode = JsonUtils.toJsonNode(schema);
        JsonNode payloadNode = payloadSchema.toJsonNode();

        // Set the property payload to the payload schema
        ObjectNode propertiesNode = (ObjectNode) schemaNode.get("properties");
        propertiesNode.set("payload", payloadNode);

        return new JsonSchema(schemaNode);
    }
}
