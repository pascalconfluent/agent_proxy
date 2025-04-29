package io.confluent.pas.agent.proxy.rest.a2a;

import io.confluent.pas.agent.common.services.schemas.Registration;
import io.confluent.pas.agent.proxy.rest.a2a.schemas.AgentCard;
import io.confluent.pas.agent.proxy.rest.a2a.schemas.Task;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Tag(name = "A2A", description = "Google A2A compliant API")
public class A2ARestController {

    public record Link(String href) {
    }

    public record Links(Link self, Link agents, Link def) {
    }

    public record A2ARegistration(Registration registration, Links[] _links) {
    }

    private final A2AAsyncServer a2aAsyncServer;

    public A2ARestController(A2AAsyncServer a2aAsyncServer) {
        this.a2aAsyncServer = a2aAsyncServer;
    }

    @GetMapping("/a2a")
    public List<A2ARegistration> getA2ARegistration() {
        return a2aAsyncServer.getRegistrations()
                .stream()
                .map(
                        registration -> new A2ARegistration(
                                registration,
                                new Links[]{new Links(
                                        new Link("/a2a/" + registration.getName()),
                                        new Link("/a2a"),
                                        new Link("/a2a/" + registration.getName() + "/.well-known/agent.json"))}
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

    @PostMapping("/a2a/{agent-name}")
    public ResponseEntity<String> processRequest(@PathVariable("agent-name") String agentName, @RequestBody Task task) {
        return ResponseEntity.ok("Hello from " + agentName);
    }
}
