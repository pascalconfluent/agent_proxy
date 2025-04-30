package io.confluent.pas.agent.proxy.frameworks.flink;

import io.confluent.pas.agent.common.services.schemas.Registration;
import io.confluent.pas.agent.proxy.frameworks.flink.subscription.SubscriptionRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.table.api.Table;
import org.apache.flink.table.api.TableEnvironment;

@Slf4j
@AllArgsConstructor
public class SubscriptionHandler<REQ, RES> {

    private final Class<REQ> requestClass;
    private final Class<RES> responseClass;
    private final TableEnvironment env;

    /**
     * Interface for handling incoming requests from Kafka topics.
     *
     * @param <REQ> Request payload type
     * @param <RES> Response payload type
     */
    public interface RequestHandler<REQ, RES> {
        void onRequest(SubscriptionRequest<REQ, RES> subscriptionRequest);
    }

    /**
     * Subscribes to a registration using derived schemas from class types.
     *
     * @param registration Registration containing topic and name information
     * @param handler      Handler to process incoming requests
     * @throws SubscriptionException if subscription setup fails
     */
    public void subscribeWith(Registration registration,
                              RequestHandler<REQ, RES> handler) throws SubscriptionException {
        log.info("Subscribing for registration: {}", registration.getName());

        try {
            Table table = env.from("_agent_registry");
        } catch (Exception e) {
            throw new SubscriptionException("Failed to subscribe with registration: " + registration.getName(), e);
        }
    }
}
