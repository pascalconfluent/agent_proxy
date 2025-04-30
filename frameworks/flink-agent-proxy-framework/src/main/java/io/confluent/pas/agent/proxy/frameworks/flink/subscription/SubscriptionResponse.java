package io.confluent.pas.agent.proxy.frameworks.flink.subscription;

import io.confluent.pas.agent.common.services.models.Key;

/**
 * Response class that holds the key and the response object
 *
 * @param <RES> Response type
 */
public record SubscriptionResponse<RES>(Key key, RES response) {
}
