package io.confluent.pas.agent.proxy.registration.handlers.mcp;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.confluent.pas.agent.common.services.schemas.Registration;
import io.confluent.pas.agent.common.utils.JsonUtils;
import io.confluent.pas.agent.proxy.registration.RequestResponseHandler;
import io.confluent.pas.agent.proxy.registration.handlers.AbstractRegistrationHandler;
import io.confluent.pas.agent.proxy.registration.schemas.RegistrationSchemas;
import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Handler for MCP (Model Context Protocol) tool registration and request
 * processing.
 * This class manages the lifecycle of MCP tools, including initialization,
 * request handling,
 * and teardown.
 */
@Slf4j
public class McpToolHandler extends AbstractRegistrationHandler<Registration, McpAsyncServer, McpSchema.CallToolResult> {

    /**
     * Constructs a new McpToolHandler instance.
     *
     * @param registration           the tool registration details
     * @param schemas                the registration schemas used for validation
     * @param requestResponseHandler the handler for processing requests and
     *                               responses
     * @param mcpServer              the MCP server instance
     */
    public McpToolHandler(Registration registration,
                          RegistrationSchemas schemas,
                          RequestResponseHandler requestResponseHandler,
                          McpAsyncServer mcpServer) {
        super(registration, schemas, mcpServer, requestResponseHandler, (payload) -> {
            try {
                final String result = JsonUtils.toString(payload);
                return new McpSchema.CallToolResult(List.of(new McpSchema.TextContent(result)), false);
            } catch (JsonProcessingException e) {
                log.error("Failed to convert response payload to string: {}", e.getMessage());
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Initializes the tool with the MCP server.
     * Creates a tool specification with appropriate schema and registers it with
     * the server.
     *
     * @return a Mono that completes when initialization is done
     */
    @Override
    public Mono<Void> initialize() {
        log.info("Registering tool {}", registration.getName());

        final McpSchema.Tool tool = createToolSchema();
        final McpServerFeatures.AsyncToolSpecification toolRegistration = createToolSpecification(tool);

        return registrationServer.addTool(toolRegistration);
    }

    /**
     * Creates the tool schema definition for MCP.
     *
     * @return the MCP tool schema
     */
    private McpSchema.Tool createToolSchema() {
        return new McpSchema.Tool(
                registration.getName(),
                registration.getDescription(),
                schemas.getRequestSchema().getPayloadSchema());
    }

    /**
     * Creates the tool specification for the MCP server.
     *
     * @param tool the tool schema definition
     * @return the async tool specification
     */
    private McpServerFeatures.AsyncToolSpecification createToolSpecification(McpSchema.Tool tool) {
        return new McpServerFeatures.AsyncToolSpecification(
                tool,
                (exchange, toolArguments) -> onRequest(toolArguments));
    }

    /**
     * Removes the tool from the MCP server.
     *
     * @return a Mono that completes when the tool is removed
     */
    @Override
    public Mono<Void> teardown() {
        log.info("Removing tool {}", registration.getName());
        return registrationServer.removeTool(registration.getName());
    }

}