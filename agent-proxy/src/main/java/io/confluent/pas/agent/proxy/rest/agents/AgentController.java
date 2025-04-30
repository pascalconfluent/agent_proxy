package io.confluent.pas.agent.proxy.rest.agents;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.confluent.pas.agent.common.services.schemas.Registration;
import io.confluent.pas.agent.common.services.schemas.ResourceRegistration;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.http.HttpMethod;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Tag(name = AgentController.TITLE, description = AgentController.DESCRIPTION)
public class AgentController {
    public final static String TITLE = "Agent";
    public final static String DESCRIPTION = "Confluent Agent API";
    public final static String AGENT_PATH = "/agents/";

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Link {
        private String href;
        private String method;
        @JsonProperty(value = "request-schema")
        private String requestSchema;
        @JsonProperty(value = "response-schema")
        private String responseSchema;

        public Link(String href, String method) {
            this.href = href;
            this.method = method;
        }
    }

    public record Links(Link self, Link agents) {
    }

    public record RestRegistration(Registration registration, Links[] _links) {
    }

    private final AgentAsyncServer agentAsyncServer;

    public AgentController(AgentAsyncServer agentAsyncServer) {
        this.agentAsyncServer = agentAsyncServer;
    }


    @GetMapping("/agents")
    public List<RestRegistration> getAgent() {
        return agentAsyncServer.getRegistrations()
                .stream()
                .map(
                        restRegistration -> {
                            Link agentLink = new Link();
                            Registration registration = restRegistration.registration();

                            if (registration instanceof ResourceRegistration resourceRegistration) {
                                agentLink.method = HttpMethod.GET.name();
                                agentLink.href = AGENT_PATH + resourceRegistration.getName() + "/" + resourceRegistration.getUrl();
                            } else {
                                agentLink.method = HttpMethod.POST.name();
                                agentLink.href = AGENT_PATH + registration.getName();
                                agentLink.requestSchema = restRegistration.schemas().getRequestSchema().getPayloadSchema();
                                agentLink.responseSchema = restRegistration.schemas().getResponseSchema().getPayloadSchema();
                            }

                            return new RestRegistration(
                                    registration,
                                    new Links[]{new Links(
                                            agentLink, new Link(AGENT_PATH, HttpMethod.GET.name()))}
                            );
                        })
                .toList();

    }

}
