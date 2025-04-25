package io.confluent.pas.agent.exemple;

import io.confluent.pas.agent.common.services.schemas.ResourceRequest;
import io.confluent.pas.agent.common.services.schemas.TextResourceResponse;
import io.confluent.pas.agent.common.utils.UriTemplate;
import io.confluent.pas.agent.proxy.frameworks.java.subscription.SubscriptionRequest;
import io.confluent.pas.agent.proxy.frameworks.java.spring.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Agent class responsible for delivering resources.
 */
@Slf4j
@Component
public class ResourceAgent {
    private final static String URI = "client/{client_id}";
    private final static String MIME_TYPE = "application/json";

    private final UriTemplate template;

    /**
     * Constructor to initialize the ResourceAgent with required dependencies.
     */
    public ResourceAgent() {
        this.template = new UriTemplate(URI);
    }

    /**
     * Handles incoming requests by processing the query and responding with a message.
     *
     * @param subscriptionRequest The incoming request containing the query.
     */
    @Resource(
            name = "resource-agent--rcs",
            description = "This agent returns resources.",
            request_topic = "resource-request",
            response_topic = "resource-response",
            contentType = MIME_TYPE,
            path = URI,
            responseClass = TextResourceResponse.class
    )
    public void onRequest(SubscriptionRequest<ResourceRequest, TextResourceResponse> subscriptionRequest) {
        log.info("Received request: {}", subscriptionRequest.getRequest().getUri());

        // Extract values from the URI using the template
        final Map<String, Object> values = this.template.match(subscriptionRequest.getRequest().getUri());

        // Respond to the request with a message containing the client_id
        subscriptionRequest.respond(new TextResourceResponse(
                        subscriptionRequest.getRequest().getUri(),
                        MIME_TYPE,
                        "{ \"message\": \"Hello, " + values.get("client_id") + "!\" }"
                ))
                .doOnError(e -> log.error("Failed to respond to request", e))
                .block();
    }
}