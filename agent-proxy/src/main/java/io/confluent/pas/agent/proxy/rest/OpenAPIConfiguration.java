package io.confluent.pas.agent.proxy.rest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.confluent.pas.agent.common.services.schemas.Registration;
import io.confluent.pas.agent.common.services.schemas.ResourceRegistration;
import io.confluent.pas.agent.common.utils.JsonUtils;
import io.confluent.pas.agent.common.utils.UriTemplate;
import io.confluent.pas.agent.proxy.registration.RegistrationCoordinator;
import io.confluent.pas.agent.proxy.registration.handlers.CompositeHandler;
import io.confluent.pas.agent.proxy.registration.schemas.RegistrationSchemas;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.http.HttpStatus;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Configuration class for OpenAPI documentation.
 * Responsible for generating and customizing OpenAPI specification by dynamically creating
 * path entries based on registered handlers. Handles two types of paths:
 * <ul>
 *   <li>Standard API paths (/api/...) for regular request-response operations</li>
 *   <li>Resource paths (/rcs/...) for resource-based operations</li>
 * </ul>
 * The class provides automatic schema generation from JSON schema definitions,
 * handles path parameters, and configures appropriate request/response formats.
 */
@Slf4j
@Configuration
public class OpenAPIConfiguration {

    private static final String API_PATH_PREFIX = "/api/";
    private static final String RESOURCE_PATH_PREFIX = "/rcs/";
    private static final String PATH_PARAM_TYPE = "string";
    private static final String PATH_PARAM_LOCATION = "path";
    private static final String MEDIA_TYPE_JSON = "application/json";

    /**
     * Record representing a JSON schema structure used for OpenAPI schema definitions.
     * Maps JSON schema attributes to Java objects to facilitate OpenAPI schema generation.
     * <ul>
     *   <li>{@code type} - The schema type (object, string, integer, etc.)</li>
     *   <li>{@code properties} - Map of property names to their schema definitions</li>
     *   <li>{@code required} - List of required property names</li>
     *   <li>{@code additionalProperties} - Whether additional properties are allowed</li>
     * </ul>
     */
    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    record JsonSchema(
            @JsonProperty("type") String type,
            @JsonProperty("properties") Map<String, Object> properties,
            @JsonProperty("required") List<String> required,
            @JsonProperty("additionalProperties") Boolean additionalProperties) {
    }

    private final RegistrationCoordinator registrationCoordinator;

    /**
     * Constructor for OpenAPIConfiguration.
     *
     * @param registrationCoordinator the registration coordinator
     */
    public OpenAPIConfiguration(RegistrationCoordinator registrationCoordinator) {
        this.registrationCoordinator = registrationCoordinator;
    }

    /**
     * Bean for OpenAPI configuration.
     *
     * @return the OpenAPI instance
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Confluent Tool API")
                        .version("1.0")
                        .license(new License().name("Apache 2.0").url("https://confluent.io")));
    }

    /**
     * Bean for customizing OpenAPI paths.
     *
     * @return the OpenApiCustomizer instance
     */
    @Bean
    public OpenApiCustomizer openApiCustomizer() {
        return openAPI -> openAPI
                .getPaths()
                .putAll(buildPathsFromRegistrations());
    }

    /**
     * Builds the OpenAPI path definitions by iterating through all registered handlers.
     * For each handler, it extracts registration information and creates appropriate path items:
     * <ul>
     *   <li>For resource registrations: Creates GET endpoints with path parameters</li>
     *   <li>For standard registrations: Creates POST endpoints with request/response schemas</li>
     * </ul>
     *
     * @return A map of API path strings to PathItem objects for the OpenAPI specification
     */
    private Map<String, PathItem> buildPathsFromRegistrations() {
        final List<CompositeHandler> registrationHandlers = registrationCoordinator
                .getAllRegistrationHandlers();
        final Map<String, PathItem> pathItems = new HashMap<>();

        registrationHandlers.forEach(handler -> {
            try {
                final Registration registration = handler.getRegistration();

                if (registration instanceof ResourceRegistration resourceRegistration) {
                    addResourcePathItem(resourceRegistration, registration, pathItems);
                } else {
                    addStandardPathItem(registration, handler.getSchemas(), pathItems);
                }
            } catch (Exception e) {
                log.error("Error building OpenAPI path for handler: {}", handler.getClass().getSimpleName(), e);
            }
        });

        return pathItems;
    }

    /**
     * Adds a standard (non-resource) path item to the path items map.
     * Creates a POST operation endpoint that:
     * <ul>
     *   <li>Uses the registration name as the path</li>
     *   <li>Configures the request body based on the request schema</li>
     *   <li>Sets up the response structure based on the response schema</li>
     *   <li>Prefixes the path with "/api/"</li>
     * </ul>
     *
     * @param registration The registration containing endpoint metadata (name, description)
     * @param schemas      The registration schemas with request/response definitions
     * @param pathItems    The path items map to be updated with the new path item
     */
    private void addStandardPathItem(Registration registration,
                                     RegistrationSchemas schemas,
                                     Map<String, PathItem> pathItems) {
        final String path = registration.getName();
        final PathItem pathItem = new PathItem();

        try {
            final Content requestBody = createRequestBody(schemas.getRequestSchema().getPayloadSchema());
            final ApiResponse response = createApiResponse(schemas.getResponseSchema().getPayloadSchema());

            Operation operation = new Operation()
                    .summary(path)
                    .operationId(path)
                    .description(registration.getDescription())
                    .requestBody(new RequestBody().content(requestBody))
                    .responses(createSuccessResponse(response));

            pathItem.operation(PathItem.HttpMethod.POST, operation);
            pathItems.put(API_PATH_PREFIX + path, pathItem);
        } catch (JsonProcessingException e) {
            log.error("Error parsing schema for path: {}", path, e);
        }
    }

    /**
     * Adds a resource-based path to the path items map.
     * Creates a GET operation endpoint that:
     * <ul>
     *   <li>Uses the resource URL pattern from the registration</li>
     *   <li>Automatically extracts and defines path parameters from the URL template</li>
     *   <li>Sets up the response with the appropriate MIME type</li>
     *   <li>Prefixes the path with "/rcs/"</li>
     * </ul>
     *
     * @param resourceRegistration The resource-specific registration with URL template and MIME type
     * @param registration         The general registration containing description metadata
     * @param pathItems            The path items map to be updated with the new path item
     */
    private void addResourcePathItem(ResourceRegistration resourceRegistration,
                                     Registration registration,
                                     Map<String, PathItem> pathItems) {
        final PathItem pathItem = new PathItem();
        final String urlPath = resourceRegistration.getUrl();

        final ApiResponse response = createApiResponse(resourceRegistration);
        Operation operation = new Operation()
                .summary(urlPath)
                .operationId(urlPath)
                .description(registration.getDescription())
                .responses(createSuccessResponse(response));

        pathItem.operation(PathItem.HttpMethod.GET, operation);

        // Process URI template parameters
        addPathParameters(pathItem, urlPath);

        pathItems.put(RESOURCE_PATH_PREFIX + urlPath, pathItem);
    }

    /**
     * Extracts and adds path parameters to the path item based on the URI template.
     * Analyzes the URI template to find template variables (e.g., {id}, {name}) and
     * creates corresponding OpenAPI parameter definitions for each variable.
     *
     * @param pathItem    The path item to update with parameter definitions
     * @param uriTemplate The URI template string containing parameter placeholders
     */
    private void addPathParameters(PathItem pathItem, String uriTemplate) {
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
     *   <li>Required (true)</li>
     *   <li>Located in the path</li>
     *   <li>Having a string type</li>
     * </ul>
     *
     * @param name The name of the parameter (matches the template variable name)
     * @return The configured OpenAPI Parameter object
     */
    private Parameter createPathParameter(String name) {
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
     * @return The ApiResponses object with the success response mapped to status 200
     */
    private ApiResponses createSuccessResponse(ApiResponse response) {
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
    private Content createRequestBody(String requestSchema) throws JsonProcessingException {
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
     *   <li>The MIME type specified in the resource registration</li>
     *   <li>An HTTP 200 OK status description</li>
     *   <li>No schema definition (since resource responses vary by resource type)</li>
     * </ul>
     *
     * @param registration The resource registration containing the MIME type
     * @return The configured ApiResponse object
     */
    private ApiResponse createApiResponse(ResourceRegistration registration) {
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
     *   <li>Appropriate content type (application/json)</li>
     *   <li>Schema structure matching the provided JSON schema</li>
     *   <li>HTTP 200 OK status description</li>
     * </ul>
     *
     * @param responseSchema The JSON schema string defining the response structure
     * @return The configured ApiResponse object
     * @throws JsonProcessingException If the response schema JSON cannot be parsed
     */
    private ApiResponse createApiResponse(String responseSchema) throws JsonProcessingException {
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
     *   <li>Sets basic properties like type and required fields</li>
     *   <li>Processes primitive types directly (string, integer, boolean, array)</li>
     *   <li>For object types, recursively processes nested properties</li>
     *   <li>Handles additionalProperties configuration</li>
     * </ul>
     *
     * @param schemaProperties The JsonSchema record with type, properties, and requirements
     * @return The configured OpenAPI Schema object
     */
    @SuppressWarnings("unchecked")
    @NotNull
    private static Schema<?> createSchemaFromProperties(JsonSchema schemaProperties) {
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
     * Processes additional schema attributes for a property and applies them to the OpenAPI Schema object.
     * Handles various property attributes including:
     * <ul>
     *   <li>format - Sets format attribute (e.g., date-time, int64)</li>
     *   <li>enum - Sets enumeration values</li>
     *   <li>items - For array types, processes the item schema definition</li>
     *   <li>description - Sets property description text</li>
     * </ul>
     *
     * @param property The property definition map containing attributes
     * @param schema   The OpenAPI Schema object to update with these attributes
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void processPropertyAttributes(Map<String, Object> property, Schema<?> schema) {
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
     *   <li>Single type as string (e.g., "type": "string")</li>
     *   <li>Multiple types as an array (e.g., "type": ["string", "null"])</li>
     *   <li>Default to "object" if no type is specified</li>
     * </ul>
     *
     * @param property The property definition map containing type information
     * @return A set of type strings found in the property definition
     */
    @SuppressWarnings("unchecked")
    private static Set<String> getPropertyTypes(Map<String, Object> property) {
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