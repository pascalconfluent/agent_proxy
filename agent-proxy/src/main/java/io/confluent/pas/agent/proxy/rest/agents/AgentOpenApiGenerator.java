package io.confluent.pas.agent.proxy.rest.agents;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.confluent.pas.agent.common.services.schemas.Registration;
import io.confluent.pas.agent.common.services.schemas.ResourceRegistration;
import io.confluent.pas.agent.proxy.registration.schemas.RegistrationSchemas;
import io.confluent.pas.agent.proxy.rest.RestUtils;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Component responsible for generating OpenAPI documentation from agent
 * registrations.
 * Converts different types of registrations into appropriate OpenAPI path items
 * and builds a complete paths map for OpenAPI specifications.
 */
@Slf4j
@Component
public class AgentOpenApiGenerator {

    /**
     * Builds OpenAPI path items from all registered endpoints.
     * This method is used to generate OpenAPI documentation for the API.
     * It converts registrations into appropriate GET or POST path items based on
     * their type.
     *
     * @param registrations List of registration items to convert to OpenAPI paths
     * @return Map of API paths to their corresponding PathItem definitions for
     * OpenAPI documentation
     */
    public Map<String, PathItem> buildPathsFromRegistrations(List<AgentAsyncServer.RegistrationsItem> registrations) {
        return registrations.stream()
                .map(this::createPathItemFromRegistration)
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Pair::getKey, Pair::getValue));
    }

    /**
     * Creates a PathItem from a registration item based on its type.
     * Dispatches to the appropriate method for creating GET or POST path items.
     *
     * @param registrationItem The registration item to convert
     * @return A pair containing the URL path and the created PathItem, or null if
     * creation fails
     */
    private Pair<String, PathItem> createPathItemFromRegistration(AgentAsyncServer.RegistrationsItem registrationItem) {
        final Registration registration = registrationItem.registration();
        if (registration instanceof ResourceRegistration resourceRegistration) {
            return toGetPathItem(resourceRegistration);
        } else {
            return toPostPathItem(registration, registrationItem.schemas());
        }
    }

    /**
     * Converts a standard API registration to a POST path item for OpenAPI
     * documentation.
     * Creates the appropriate request body, response definitions, and operation
     * metadata.
     *
     * @param registration The registration to convert
     * @param schemas      Schema definitions for the registration
     * @return A pair containing the URL path and the created PathItem, or null if
     * creation fails
     */
    private Pair<String, PathItem> toPostPathItem(Registration registration,
                                                  RegistrationSchemas schemas) {
        final String path = registration.getName();
        final PathItem pathItem = new PathItem();

        try {
            final Content requestBody = RestUtils.createRequestBody(schemas.getRequestSchema().getPayloadSchema());
            final ApiResponse response = RestUtils.createApiResponse(schemas.getResponseSchema().getPayloadSchema());

            Operation operation = new Operation()
                    .tags(List.of(AgentController.TITLE))
                    .summary(path)
                    .operationId(path)
                    .description(registration.getDescription())
                    .requestBody(new RequestBody().content(requestBody))
                    .responses(RestUtils.createSuccessResponse(response));

            pathItem.operation(PathItem.HttpMethod.POST, operation);
            return Pair.of(RestUtils.getUrl(AgentController.AGENT_PATH, registration), pathItem);
        } catch (JsonProcessingException e) {
            log.error("Error parsing schema for path: {}", path, e);
            return null;
        }
    }

    /**
     * Converts a resource registration to a GET path item for OpenAPI
     * documentation.
     * Creates the appropriate response definitions, operation metadata, and path
     * parameters.
     *
     * @param resourceRegistration The resource registration to convert
     * @return A pair containing the URL path and the created PathItem
     */
    private Pair<String, PathItem> toGetPathItem(ResourceRegistration resourceRegistration) {
        final PathItem pathItem = new PathItem();
        final String urlPath = resourceRegistration.getUrl();

        final ApiResponse response = RestUtils.createApiResponse(resourceRegistration);
        Operation operation = new Operation()
                .summary(urlPath)
                .tags(List.of(AgentController.TITLE))
                .operationId(urlPath)
                .description(resourceRegistration.getDescription())
                .responses(RestUtils.createSuccessResponse(response));

        pathItem.operation(PathItem.HttpMethod.GET, operation);

        // Process URI template parameters
        RestUtils.addPathParameters(pathItem, urlPath);

        return Pair.of(RestUtils.getUrl(AgentController.AGENT_PATH, resourceRegistration), pathItem);
    }
}