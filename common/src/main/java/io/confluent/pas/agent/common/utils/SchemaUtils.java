package io.confluent.pas.agent.common.utils;

import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaVersion;
import io.confluent.kafka.schemaregistry.ParsedSchema;
import io.confluent.kafka.schemaregistry.annotations.Schema;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.rest.entities.SchemaReference;
import io.confluent.kafka.schemaregistry.client.rest.entities.requests.RegisterSchemaResponse;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import io.confluent.kafka.schemaregistry.json.JsonSchema;
import io.confluent.kafka.schemaregistry.json.JsonSchemaProvider;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility class for JSON schema operations, including schema generation and
 * registration.
 * Provides methods to generate JSON schemas from Java classes and interact with
 * Confluent's Schema Registry.
 */
@Slf4j
public class SchemaUtils {

    private static final JsonSchemaProvider SCHEMA_PROVIDER = new JsonSchemaProvider();

    /**
     * Registers a schema if it doesn't already exist in the Schema Registry.
     *
     * @param topicName            Topic name for which to register the schema
     * @param clazz                Class to generate schema from
     * @param forKey               If true, registers schema for the key; if false,
     *                             for the value
     * @param schemaRegistryClient Schema registry client instance
     */
    public static void registerSchemaIfMissing(String topicName,
                                               Class<?> clazz,
                                               boolean forKey,
                                               SchemaRegistryClient schemaRegistryClient) {
        try {
            if (!isSchemaRegistered(topicName, forKey, schemaRegistryClient)) {
                registerSchema(topicName, clazz, forKey, schemaRegistryClient);
            }
        } catch (IOException | RestClientException e) {
            log.error("Failed to register schema", e);
        }
    }

    /**
     * Checks if a schema is already registered in the Schema Registry.
     *
     * @param topicName            Topic name to check
     * @param forKey               If true, checks for key schema; if false, for
     *                             value schema
     * @param schemaRegistryClient Schema registry client instance
     * @return true if the schema is registered, false otherwise
     * @throws IOException         If there's an issue connecting to the Schema
     *                             Registry
     * @throws RestClientException If the Schema Registry returns an error
     */
    public static boolean isSchemaRegistered(String topicName,
                                             boolean forKey,
                                             SchemaRegistryClient schemaRegistryClient) throws IOException, RestClientException {
        final String subject = topicName + (forKey ? "-key" : "-value");
        try {
            return schemaRegistryClient.getLatestSchemaMetadata(subject) != null;
        } catch (RestClientException e) {
            if (e.getStatus() == 404) {
                return false;
            }

            throw e;
        }
    }

    /**
     * Registers a schema for a topic using a Java class to generate the schema.
     *
     * @param topicName            Topic name for which to register the schema
     * @param clazz                Class to generate schema from
     * @param forKey               If true, registers schema for the key; if false,
     *                             for the value
     * @param schemaRegistryClient Schema registry client instance
     * @throws IOException         If schema generation fails
     * @throws RestClientException If schema registration fails
     */
    public static void registerSchema(String topicName,
                                      Class<?> clazz,
                                      boolean forKey,
                                      SchemaRegistryClient schemaRegistryClient) throws IOException, RestClientException {
        final JsonSchema jsonSchema;
        try {
            jsonSchema = getJsonSchema(clazz);
        } catch (IOException e) {
            log.error("Failed to generate schema", e);
            throw e;
        }

        registerSchema(topicName, jsonSchema, forKey, schemaRegistryClient);
    }

    /**
     * Registers a pre-generated JSON schema for a topic.
     *
     * @param topicName            Topic name for which to register the schema
     * @param jsonSchema           Pre-generated JSON schema to register
     * @param forKey               If true, registers schema for the key; if false,
     *                             for the value
     * @param schemaRegistryClient Schema registry client instance
     * @throws IOException         If there's an issue connecting to the Schema
     *                             Registry
     * @throws RestClientException If schema registration fails
     */
    public static void registerSchema(String topicName,
                                      JsonSchema jsonSchema,
                                      boolean forKey,
                                      SchemaRegistryClient schemaRegistryClient) throws IOException, RestClientException {
        // Then register the schema
        try {
            final String subject = topicName + (forKey ? "-key" : "-value");
            final List<ParsedSchema> parsedSchemas = schemaRegistryClient.getSchemas(subject, false, true);
            if (!parsedSchemas.isEmpty()) {
                log.info("Schema already registered for subject {}", subject);
                return;
            }

            RegisterSchemaResponse response = schemaRegistryClient.registerWithResponse(
                    subject,
                    jsonSchema,
                    false);
            log.info("Registered schema with id {} for subject {}", response.getId(), subject);
        } catch (IOException | RestClientException e) {
            log.error("Failed to register schema", e);
            throw e;
        }
    }

    /**
     * Gets the JSON schema for a Java class.
     * First checks if the class has a {@link Schema} annotation; if not, generates
     * the schema dynamically.
     *
     * @param clazz Class to get schema for
     * @return JsonSchema representation
     * @throws IOException If schema generation fails
     */
    public static JsonSchema getJsonSchema(Class<?> clazz) throws IOException {
        Schema schemaAnnotation = getSchemaAnnotation(clazz);

        String schema = (schemaAnnotation == null)
                ? generateSchemaFromClass(clazz)
                : schemaAnnotation.value();
        List<SchemaReference> references = (schemaAnnotation == null)
                ? new ArrayList<>()
                : Arrays.stream(schemaAnnotation.refs())
                .map(ref -> new SchemaReference(ref.name(), ref.subject(), ref.version()))
                .collect(Collectors.toList());

        return (JsonSchema) SCHEMA_PROVIDER.parseSchema(schema, references, false, false)
                .orElseThrow(() -> new IOException("Invalid schema " + schema
                        + " with refs " + references));
    }

    /**
     * Gets the schema value as a String for a Java class.
     * First checks if the class has a {@link Schema} annotation; if not, generates
     * the schema dynamically.
     *
     * @param clazz Class to get schema for
     * @return Schema as String
     */
    public static String getSchema(Class<?> clazz) {
        Schema schema = getSchemaAnnotation(clazz);
        if (schema == null) {
            return generateSchemaFromClass(clazz);
        } else {
            return schema.value();
        }
    }

    /**
     * Retrieves the Schema annotation from a class if present.
     *
     * @param clazz Class to check for Schema annotation
     * @return Schema annotation if present, null otherwise
     */
    private static Schema getSchemaAnnotation(Class<?> clazz) {
        if (!clazz.isAnnotationPresent(Schema.class)) {
            return null;
        }

        return clazz.getAnnotation(Schema.class);
    }

    /**
     * Dynamically generates a JSON schema from a Java class using victools.
     * Uses Draft 2020-12 JSON Schema specification.
     *
     * @param clazz Class to generate schema from
     * @return JSON schema as String
     */
    private static String generateSchemaFromClass(Class<?> clazz) {
        SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2020_12);
        SchemaGenerator generator = new SchemaGenerator(configBuilder.build());

        return generator.generateSchema(clazz).toString();
    }
}
