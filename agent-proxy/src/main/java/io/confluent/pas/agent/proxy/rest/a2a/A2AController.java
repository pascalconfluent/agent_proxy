package io.confluent.pas.agent.proxy.rest.a2a;

import io.confluent.pas.agent.common.services.schemas.Registration;
import io.confluent.pas.agent.proxy.rest.a2a.schemas.AgentCard;
import io.confluent.pas.agent.proxy.rest.a2a.schemas.Task;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

//@RestController
@Tag(name = "A2A", description = "Google A2A compliant API")
public class A2AController {
    private final static String AGENT_PATH = "/a2a/";

    public record Link(String href) {
    }

    public record Links(Link self, Link agents, Link def) {
    }

    public record A2ARegistration(Registration registration, Links[] _links) {
    }

    private final A2AAsyncServer a2aAsyncServer;

    public A2AController(A2AAsyncServer a2aAsyncServer) {
        this.a2aAsyncServer = a2aAsyncServer;
    }

    @GetMapping(AGENT_PATH)
    public List<A2ARegistration> getA2ARegistration() {
        return a2aAsyncServer.getRegistrations()
                .stream()
                .map(
                        registration -> new A2ARegistration(
                                registration,
                                new Links[]{new Links(
                                        new Link(AGENT_PATH + registration.getName()),
                                        new Link(AGENT_PATH),
                                        new Link(AGENT_PATH + registration.getName() + "/.well-known/agent.json"))}
                        ))
                .toList();
    }

    @GetMapping("/a2a/{agent-name}/.well-known/agent.json")
    public ResponseEntity<AgentCard> getAgentCard(@PathVariable("agent-name") String agentName) {
        final AgentCard agentCard = a2aAsyncServer.getAgentCards(agentName);
        if (agentCard == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(agentCard);
    }

    @PostMapping(AGENT_PATH + "{agent-name}")
    public ResponseEntity<Task> processRequest(@PathVariable("agent-name") String agentName, @RequestBody Task task) {
        return ResponseEntity.ok(task);
    }
}
