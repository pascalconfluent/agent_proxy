package io.confluent.pas.agent.proxy.rest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.confluent.pas.agent.common.services.schemas.Registration;
import io.confluent.pas.agent.common.services.schemas.ResourceRegistration;
import io.confluent.pas.agent.common.utils.JsonUtils;
import io.confluent.pas.agent.common.utils.UriTemplate;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Utility class for REST API operations in the agent proxy.
 * Provides methods for constructing URLs, handling OpenAPI specifications,
 * managing path parameters, and creating API request/response structures.
 * <p>
 * This utility primarily supports the dynamic registration and exposure of
 * API endpoints based on agent and resource registrations.
 */
public class RestUtils {
    private static final String PATH_PARAM_TYPE = "string";
    private static final String PATH_PARAM_LOCATION = "path";
    private static final String MEDIA_TYPE_JSON = "application/json";

    /**
     * Record representing a JSON schema structure used for OpenAPI schema
     * definitions.
     * Maps JSON schema attributes to Java objects to facilitate OpenAPI schema
     * generation.
     * <ul>
     * <li>{@code type} - The schema type (object, string, integer, etc.)</li>
     * <li>{@code properties} - Map of property names to their schema
     * definitions</li>
     * <li>{@code required} - List of required property names</li>
     * <li>{@code additionalProperties} - Whether additional properties are
     * allowed</li>
     * </ul>
     */
    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record JsonSchema(
            @JsonProperty("type") String type,
            @JsonProperty("properties") Map<String, Object> properties,
            @JsonProperty("required") List<String> required,
            @JsonProperty("additionalProperties") Boolean additionalProperties) {
    }

    /**
     * Constructs the URL for a given registration.
     * Combines the prefix with the registration name, ensuring proper path
     * formatting.
     *
     * @param prefix       The URL prefix (e.g., "/agents/")
     * @param registration The registration object containing the name
     * @return The constructed URL string
     */
    public static String getUrl(String prefix, Registration registration) {
        // check if the prefix ends with "/"
        if (prefix.endsWith("/")) {
            return prefix + registration.getName();
        }

        return prefix + "/" + registration.getName();
    }

    /**
     * Constructs the URL for a given resource registration.
     * Combines the prefix with the registration name and URL, ensuring proper path
     * formatting.
     *
     * @param prefix       The URL prefix (e.g., "/rcs/")
     * @param registration The resource registration object containing the name and
     *                     URL
     * @return The constructed URL string
     */
    public static String getUrl(String prefix, ResourceRegistration registration) {
        // check if the prefix ends with "/"
        if (prefix.endsWith("/")) {
            return prefix + registration.getName() + "/" + registration.getUrl();
        }

        return prefix + "/" + registration.getName() + "/" + registration.getUrl();
    }

    /**
     * Extracts and adds path parameters to the path item based on the URI template.
     * Analyzes the URI template to find template variables (e.g., {id}, {name}) and
     * creates corresponding OpenAPI parameter definitions for each variable.
     *
     * @param pathItem    The path item to update with parameter definitions
     * @param uriTemplate The URI template string containing parameter placeholders
     */
    public static void addPathParameters(PathItem pathItem, String uriTemplate) {
        UriTemplate template = new UriTemplate(uriTemplate);
        template.getParts().stream()
                .filter(part -> part instanceof UriTemplate.TemplatePart)
                .map(part -> (UriTemplate.TemplatePart) part)
                .flatMap(part -> part.getNames().stream())
                .forEach(name -> pathItem.addParametersItem(createPathParameter(name)));
    }

    /**
     * Creates an OpenAPI path parameter definition with the given name.
     * Configures the parameter as:
     * <ul>
     * <li>Required (true)</li>
     * <li>Located in the path</li>
     * <li>Having a string type</li>
     * </ul>
     *
     * @param name The name of the parameter (matches the template variable name)
     * @return The configured OpenAPI Parameter object
     */
    public static Parameter createPathParameter(String name) {
        return new Parameter()
                .name(name)
                .in(PATH_PARAM_LOCATION)
                .required(true)
                .schema(new Schema<>().type(PATH_PARAM_TYPE));
    }

    /**
     * Creates a success response wrapper (HTTP 200 OK) for an API response.
     * Wraps the given API response in an ApiResponses container with the
     * appropriate HTTP status code.
     *
     * @param response The API response to be included as the success response
     * @return The ApiResponses object with the success response mapped to status
     * 200
     */
    public static ApiResponses createSuccessResponse(ApiResponse response) {
        return new ApiResponses().addApiResponse(
                String.valueOf(HttpStatus.OK.value()),
                response);
    }

    /**
     * Creates an OpenAPI request body content definition from a JSON schema string.
     * Parses the JSON schema definition into properties and creates a structured
     * OpenAPI schema object with the appropriate content type (application/json).
     *
     * @param requestSchema The JSON schema string defining the request structure
     * @return The Content object for the OpenAPI request body definition
     * @throws JsonProcessingException If the request schema JSON cannot be parsed
     */
    public static Content createRequestBody(String requestSchema) throws JsonProcessingException {
        final JsonSchema requestSchemaProperties = JsonUtils.toObject(requestSchema, JsonSchema.class);
        final Content requestBody = new Content();
        requestBody.addMediaType(MEDIA_TYPE_JSON,
                new MediaType().schema(createSchemaFromProperties(requestSchemaProperties)));
        return requestBody;
    }

    /**
     * Creates an OpenAPI response definition for a resource registration.
     * Configures the response with:
     * <ul>
     * <li>The MIME type specified in the resource registration</li>
     * <li>An HTTP 200 OK status description</li>
     * <li>No schema definition (since resource responses vary by resource
     * type)</li>
     * </ul>
     *
     * @param registration The resource registration containing the MIME type
     * @return The configured ApiResponse object
     */
    public static ApiResponse createApiResponse(ResourceRegistration registration) {
        final Content content = new Content();
        content.addMediaType(registration.getMimeType(), new MediaType());

        final ApiResponse response = new ApiResponse();
        response.content(content);
        response.description(HttpStatus.OK.getReasonPhrase());
        return response;
    }

    /**
     * Creates an OpenAPI response definition from a JSON schema string.
     * Parses the JSON schema definition into properties and creates a structured
     * OpenAPI schema object with:
     * <ul>
     * <li>Appropriate content type (application/json)</li>
     * <li>Schema structure matching the provided JSON schema</li>
     * <li>HTTP 200 OK status description</li>
     * </ul>
     *
     * @param responseSchema The JSON schema string defining the response structure
     * @return The configured ApiResponse object
     * @throws JsonProcessingException If the response schema JSON cannot be parsed
     */
    public static ApiResponse createApiResponse(String responseSchema) throws JsonProcessingException {
        final JsonSchema responseSchemaProperties = JsonUtils.toObject(responseSchema, JsonSchema.class);

        final Content content = new Content()
                .addMediaType(MEDIA_TYPE_JSON, new MediaType()
                        .schema(createSchemaFromProperties(responseSchemaProperties)));

        return new ApiResponse()
                .content(content)
                .description(HttpStatus.OK.getReasonPhrase());
    }

    /**
     * Converts a JsonSchema record into an OpenAPI Schema object.
     * Handles various schema types and configurations:
     * <ul>
     * <li>Sets basic properties like type and required fields</li>
     * <li>Processes primitive types directly (string, integer, boolean, array)</li>
     * <li>For object types, recursively processes nested properties</li>
     * <li>Handles additionalProperties configuration</li>
     * </ul>
     *
     * @param schemaProperties The JsonSchema record with type, properties, and
     *                         requirements
     * @return The configured OpenAPI Schema object
     */
    @SuppressWarnings("unchecked")
    @NotNull
    public static Schema<?> createSchemaFromProperties(JsonSchema schemaProperties) {
        Schema<?> schema = new Schema<>();
        schema.setType(schemaProperties.type);

        if (schemaProperties.required != null) {
            schema.setRequired(schemaProperties.required);
        }

        // Handle primitive types
        switch (schemaProperties.type) {
            case "string" -> schema.setType("string");
            case "integer" -> schema.setType("integer");
            case "boolean" -> schema.setType("boolean");
            case "array" -> schema.setType("array");
            default -> {
                if (schemaProperties.properties != null) {
                    schema.setProperties(schemaProperties.properties.entrySet().stream()
                            .collect(Collectors.toMap(
                                    Map.Entry::getKey,
                                    entry -> {
                                        final Map<String, Object> property = (Map<String, Object>) entry.getValue();
                                        final Schema<?> propertySchema = new Schema<>();
                                        final Set<String> types = getPropertyTypes(property);

                                        if (types.size() == 1) {
                                            propertySchema.setType(types.iterator().next());
                                        }

                                        processPropertyAttributes(property, propertySchema);
                                        return propertySchema;
                                    })));
                }

                if (schemaProperties.additionalProperties != null) {
                    schema.setAdditionalProperties(schemaProperties.additionalProperties);
                }
            }
        }

        return schema;
    }

    /**
     * Processes additional schema attributes for a property and applies them to the
     * OpenAPI Schema object.
     * Handles various property attributes including:
     * <ul>
     * <li>format - Sets format attribute (e.g., date-time, int64)</li>
     * <li>enum - Sets enumeration values</li>
     * <li>items - For array types, processes the item schema definition</li>
     * <li>description - Sets property description text</li>
     * </ul>
     *
     * @param property The property definition map containing attributes
     * @param schema   The OpenAPI Schema object to update with these attributes
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void processPropertyAttributes(Map<String, Object> property, Schema<?> schema) {
        // Process format if available
        if (property.containsKey("format")) {
            schema.setFormat((String) property.get("format"));
        }

        // Process enum if available
        if (property.containsKey("enum")) {
            // Use raw type to handle the generic type mismatch
            schema.setEnum((List) property.get("enum"));
        }

        // Process items for an array type
        if (property.containsKey("items") && "array".equals(schema.getType())) {
            final Map<String, Object> items = (Map<String, Object>) property.get("items");
            final Schema<?> itemSchema = new Schema<>();

            if (items.containsKey("type")) {
                itemSchema.setType((String) items.get("type"));
            }

            schema.setItems(itemSchema);
        }

        // Process description if available
        if (property.containsKey("description")) {
            schema.setDescription((String) property.get("description"));
        }
    }

    /**
     * Extracts type information from a property definition.
     * Handles different representations of types in JSON Schema:
     * <ul>
     * <li>Single type as string (e.g., "type": "string")</li>
     * <li>Multiple types as an array (e.g., "type": ["string", "null"])</li>
     * <li>Default to "object" if no type is specified</li>
     * </ul>
     *
     * @param property The property definition map containing type information
     * @return A set of type strings found in the property definition
     */
    @SuppressWarnings("unchecked")
    public static Set<String> getPropertyTypes(Map<String, Object> property) {
        if (property.containsKey("type")) {
            final Object type = property.get("type");
            if (type instanceof String) {
                return Set.of((String) type);
            } else if (type instanceof List) {
                return new HashSet<>((List<String>) type);
            }
        }
        return Set.of("object");
    }
}
