package io.confluent.pas.agent.exemple;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.service.AiServices;
import io.confluent.pas.agent.proxy.frameworks.java.subscription.SubscriptionRequest;
import io.confluent.pas.agent.proxy.frameworks.java.spring.annotation.Agent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Agent class responsible for handling sentiment analysis requests.
 */
@Slf4j
@Component
public class AgentInstance {

    private final Assistant assistant;

    /**
     * Constructor to initialize the AgentInstance with the necessary configuration.
     *
     * @param geminiKey    The API key for the Google AI Gemini model.
     * @param systemPrompt The system prompt to be used by the assistant.
     */
    public AgentInstance(@Value("${model.gemini-key}") String geminiKey,
                         @Value("${model.system-prompt}") String systemPrompt) {
        // Create a ChatLanguageModel using the Google AI Gemini model
        final ChatLanguageModel chatLanguageModel = GoogleAiGeminiChatModel.builder()
                .apiKey(geminiKey)
                .modelName("gemini-2.0-flash")
                .build();

        // Build the assistant using the AI services
        assistant = AiServices.builder(Assistant.class)
                .chatLanguageModel(chatLanguageModel)
                .systemMessageProvider((val) -> systemPrompt)
                .build();
    }

    /**
     * Handles incoming requests by processing the query and responding with the sentiment analysis result.
     *
     * @param subscriptionRequest The incoming request containing the query.
     */
    @Agent(
            name = "sample_java_agent",
            description = "A sample Java agent that performs sentiment analysis using the Google AI Gemini model.",
            requestClass = AgentQuery.class,
            responseClass = AgentResponse.class
    )
    public void onRequest(SubscriptionRequest<AgentQuery, AgentResponse> subscriptionRequest) {
        log.info("Received request: {}", subscriptionRequest.getRequest().query());

        // Process the query using the assistant and get the response
        final String response = assistant.chat(subscriptionRequest.getRequest().query());

        // Respond to the request with the sentiment analysis result
        subscriptionRequest.respond(new AgentResponse(response))
                .doOnError(e -> log.error("Failed to respond", e))
                .block();
    }
}