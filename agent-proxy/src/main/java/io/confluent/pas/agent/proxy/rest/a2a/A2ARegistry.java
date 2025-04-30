package io.confluent.pas.agent.proxy.rest.a2a;

import io.confluent.pas.agent.proxy.rest.a2a.schemas.AgentCapabilities;
import io.confluent.pas.agent.proxy.rest.a2a.schemas.AgentCard;
import io.confluent.pas.agent.proxy.rest.a2a.schemas.AgentSkill;
import io.confluent.pas.agent.common.services.schemas.Registration;
import io.confluent.pas.agent.common.services.schemas.ResourceRegistration;
import io.confluent.pas.agent.proxy.rest.RestUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Registry service for managing Agent configurations and capabilities.
 * This class maintains a collection of AgentCards that describe available agents
 * and their associated capabilities, skills, and configurations.
 */
public class A2ARegistry {

    /**
     * Thread-safe map storing agent configurations, indexed by agent name.
     * Uses ConcurrentHashMap to support concurrent access from multiple threads.
     */
    private final Map<String, AgentCard> agentCards = new ConcurrentHashMap<>();

    /**
     * Returns all registered agent cards.
     *
     * @return Iterable collection of all AgentCard instances
     */
    public Iterable<AgentCard> getAgentCards() {
        return agentCards.values();
    }

    /**
     * Retrieves a specific agent card by name.
     *
     * @param name The name of the agent to retrieve
     * @return The AgentCard for the specified agent, or null if not found
     */
    public AgentCard getAgentCard(String name) {
        return agentCards.get(name);
    }

    /**
     * Registers a new agent in the registry based on the provided registration information.
     * Creates and configures an AgentCard with capabilities, input/output modes, and skills.
     *
     * @param registration The registration details for the agent
     */
    public void registerAgent(Registration registration) {
        AgentCard agentCard = new AgentCard();
        agentCard.setName(registration.getName());
        agentCard.setDescription(registration.getDescription());
        agentCard.setVersion(registration.getVersion());
        agentCard.setCapabilities(new AgentCapabilities(true, true, false));
        agentCard.setDefaultInputModes(List.of("application/json"));

        if (registration instanceof ResourceRegistration resourceRegistration) {
            //TODO: Maybe we should not use the resource registration here
            agentCard.setDefaultOutputModes(List.of(resourceRegistration.getMimeType()));
        } else {
            agentCard.setDefaultOutputModes(List.of("application/json"));
        }
        agentCard.setUrl(RestUtils.getUrl("/a2a/", registration));

        final AgentSkill agentSkill = AgentSkill
                .builder()
                .id("ak-" + registration.getName())
                .name(registration.getName())
                .description(registration.getDescription())
                .tags(List.of("agent", "a2a"))
                //.examples()
                .build();

        agentCard.setSkills(List.of(agentSkill));

        // TODO: Add support for
//        agentCard.setProvider(registration.getProvider());
//        agentCard.setDocumentationUrl(registration.getDocumentationUrl());
//        agentCard.setAuthentication(registration.getAuthentication());


        agentCards.put(agentCard.getName(), agentCard);
    }

}
