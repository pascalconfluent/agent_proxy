package io.confluent.pas.agent.proxy.rest;

import io.confluent.pas.agent.common.services.schemas.Registration;
import io.confluent.pas.agent.common.services.schemas.ResourceRegistration;
import io.confluent.pas.agent.proxy.registration.RegistrationCoordinator;
import io.confluent.pas.agent.proxy.registration.handlers.CompositeHandler;
import io.confluent.pas.agent.proxy.registration.handlers.RegistrationHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.web.reactive.function.server.RequestPredicates.accept;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Configuration(proxyBeanMethods = false)
public class ToolRestConfiguration {

    /**
     * Creates the route for handling tool requests.
     *
     * @param registrationCoordinator the registration coordinator
     * @param toolRestController      the tool REST controller
     * @return the router function for handling tool requests
     */
    @Bean
    public RouterFunction<ServerResponse> createRoute(RegistrationCoordinator registrationCoordinator,
                                                      ToolRestController toolRestController) {
        final RouterFunctions.Builder route = route().POST(
                "/api/{toolName}",
                accept(APPLICATION_JSON),
                toolRestController::processRequest);

        // Create a route for all GET requests to the resource handlers
        final List<CompositeHandler> registrationHandlers = registrationCoordinator.getAllRegistrationHandlers();
        registrationHandlers.stream()
                .map(RegistrationHandler::getRegistration)
                .filter(Registration::isResource)
                .map(r -> (ResourceRegistration) r)
                .forEach(registration -> {
                    final String url = registration.getUrl();

                    route.GET("/rcs/" + url,
                            accept(APPLICATION_JSON),
                            toolRestController::processGetRequest);
                });

        return route.build();
    }
}
