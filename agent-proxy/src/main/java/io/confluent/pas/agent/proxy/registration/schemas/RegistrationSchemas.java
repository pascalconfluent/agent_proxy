package io.confluent.pas.agent.proxy.registration.schemas;

import com.fasterxml.jackson.databind.JsonNode;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import io.confluent.pas.agent.common.services.schemas.Registration;
import io.confluent.pas.agent.common.utils.JsonUtils;
import io.confluent.pas.agent.common.utils.Lazy;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
public class RegistrationSchemas {

    private final Lazy<RegistrationSchema> requestSchema;
    private final Lazy<RegistrationSchema> responseSchema;

    public RegistrationSchema getRequestSchema() {
        return requestSchema.get();
    }

    public RegistrationSchema getResponseSchema() {
        return responseSchema.get();
    }

    public RegistrationSchemas(SchemaRegistryClient client,
                               Registration registration) throws IOException, RestClientException {
        this.requestSchema = new Lazy<>(() -> {
            try {
                return getSchema(registration.getRequestTopicName(), client);
            } catch (RestClientException | IOException e) {
                log.error("Failed to get request schema", e);
                throw new RuntimeException(e);
            }
        });
        this.responseSchema = new Lazy<>(() -> {
            try {
                return getSchema(registration.getResponseTopicName(), client);
            } catch (RestClientException | IOException e) {
                log.error("Failed to get response schema", e);
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Get the schema for a topic
     *
     * @param topicName The topic name
     * @return The schema
     * @throws RestClientException If the schema cannot be retrieved
     * @throws IOException         If the schema cannot be retrieved
     */
    private static RegistrationSchema getSchema(String topicName,
                                                SchemaRegistryClient schemaRegistryClient) throws RestClientException, IOException {
        final String subject = topicName + "-value";
        final String schema = schemaRegistryClient.getLatestSchemaMetadata(subject).getSchema();


        return new RegistrationSchema(schema);
    }
}
