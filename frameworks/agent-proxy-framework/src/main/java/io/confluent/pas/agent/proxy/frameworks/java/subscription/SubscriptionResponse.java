package io.confluent.pas.agent.proxy.frameworks.java.subscription;

import io.confluent.pas.agent.proxy.frameworks.java.models.Key;

/**
 * Response class that holds the key and the response object
 *
 * @param <RES> Response type
 */
public record SubscriptionResponse<RES>(Key key, RES response) {
}
