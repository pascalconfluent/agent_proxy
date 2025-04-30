package io.confluent.pas.agent.proxy.frameworks.flink.subscription;

import io.confluent.pas.agent.common.services.models.Key;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.function.Consumer;

/**
 * Request class that holds the key and the request object
 *
 * @param <REQ> Request type
 * @param <RES> Response type
 */
@Slf4j
@AllArgsConstructor
public class SubscriptionRequest<REQ, RES> {
    @Getter
    private final Key key;
    @Getter
    private final REQ request;
    private final Consumer<SubscriptionResponse<RES>> responseConsumer;

    /**
     * Respond to the request
     *
     * @param subscriptionResponse Response object
     */
    public Mono<Void> respond(SubscriptionResponse<RES> subscriptionResponse) {
        return Mono.create(sink -> {
            try {
                responseConsumer.accept(subscriptionResponse);
                sink.success();
            } catch (Exception e) {
                log.error("Error responding to request", e);
                sink.error(e);
            }
        });
    }

    /**
     * Respond to the request
     *
     * @param response Response object
     */
    public Mono<Void> respond(RES response) {
        return this.respond(new SubscriptionResponse<>(key, response));
    }
}
