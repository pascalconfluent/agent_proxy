package io.confluent.pas.agent.proxy.rest.agents;

import io.confluent.pas.agent.common.services.schemas.Registration;
import io.confluent.pas.agent.common.services.schemas.ResourceRegistration;
import io.confluent.pas.agent.proxy.registration.RegistrationCoordinator;
import io.confluent.pas.agent.proxy.registration.handlers.CompositeHandler;
import io.confluent.pas.agent.proxy.registration.handlers.RegistrationHandler;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.web.reactive.function.server.RequestPredicates.accept;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

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
@Configuration(proxyBeanMethods = false)
public class AgentConfiguration {

    private final AgentAsyncServer agentAsyncServer;

    /**
     * Constructor for OpenAPIConfiguration.
     *
     * @param agentAsyncServer The RestAsyncServer instance to be used for REST registration handling
     */
    public AgentConfiguration(AgentAsyncServer agentAsyncServer) {
        this.agentAsyncServer = agentAsyncServer;
    }

    @Bean
    public GroupedOpenApi.Builder myAPIBuilder() {
        return GroupedOpenApi.builder()
                .group("AgentRestController.TITLE")
                .displayName("AgentRestController.TITLE")
                .pathsToMatch("/agents/**");
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
                        .title("Confluent Agent Proxy API")
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
        return openAPI -> {
            openAPI.getPaths()
                    .putAll(agentAsyncServer.buildPathsFromRegistrations());
        };
    }

    /**
     * Creates the route for handling tool requests.
     *
     * @param registrationCoordinator the registration coordinator
     * @return the router function for handling tool requests
     */
    @Bean
    public RouterFunction<ServerResponse> createRoute(RegistrationCoordinator registrationCoordinator) {
        final RouterFunctions.Builder route = route().POST(
                "/agents/{toolName}",
                accept(APPLICATION_JSON),
                agentAsyncServer::processRequest);

        // Create a route for all GET requests to the resource handlers
        final List<CompositeHandler> registrationHandlers = registrationCoordinator.getAllRegistrationHandlers();
        registrationHandlers.stream()
                .map(RegistrationHandler::getRegistration)
                .filter(Registration::isResource)
                .map(r -> (ResourceRegistration) r)
                .forEach(registration -> {
                    final String url = registration.getUrl();

                    route.GET("/agents/" + registration.getName() + "/" + url,
                            accept(APPLICATION_JSON),
                            agentAsyncServer::processGetRequest);
                });

        return route.build();
    }
}