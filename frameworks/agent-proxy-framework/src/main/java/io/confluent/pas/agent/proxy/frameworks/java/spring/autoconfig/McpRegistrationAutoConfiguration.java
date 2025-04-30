package io.confluent.pas.agent.proxy.frameworks.java.spring.autoconfig;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

import io.confluent.pas.agent.common.services.schemas.Registration;
import static io.confluent.pas.agent.common.services.schemas.Registration.CORRELATION_ID_FIELD_NAME;

/**
 * Auto-configuration class for MCP (Model Control Protocol) agent registration.
 * Provides configuration for registering agents in the MCP ecosystem.
 * Only activated when the 'agent.name' property is present.
 */
@Slf4j
@AutoConfiguration
@AutoConfigureOrder
@ConditionalOnProperty(prefix = "agent", name = "name")
public class McpRegistrationAutoConfiguration {

    /**
     * Name identifier for the agent
     */
    @Value("${agent.name}")
    private String name;

    /**
     * Human-readable description of the agent's purpose and capabilities
     */
    @Value("${agent.description}")
    private String agentDescription;

    /**
     * Kafka topic name where the agent listens for incoming requests
     */
    @Value("${agent.request-topic}")
    private String requestTopic;

    /**
     * Kafka topic name where the agent publishes responses
     */
    @Value("${agent.response-topic}")
    private String responseTopic;

    /**
     * Creates a Registration bean with the agent's configuration.
     * Contains all necessary information for registering the agent in MCP.
     *
     * @return Configured Registration instance
     */
    @Bean
    @ConditionalOnMissingBean
    public Registration getRegistration() {
        return Registration.builder()
                .name(name)
                .description(agentDescription)
                .requestTopicName(requestTopic)
                .responseTopicName(responseTopic)
                .build();
    }
}