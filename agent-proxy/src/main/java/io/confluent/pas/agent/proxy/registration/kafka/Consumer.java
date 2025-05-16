package io.confluent.pas.agent.proxy.registration.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;

import java.io.Closeable;
import java.util.Collection;


/**
 * Interface for managing Kafka topic subscriptions and message consumption.
 *
 * @param <K> Key type for Kafka messages
 * @param <V> Value type for Kafka messages
 */
public interface Consumer<K, V> extends Closeable {
    /**
     * Functional interface for checking timeouts in message processing.
     */
    @FunctionalInterface
    interface TimeoutChecker {
        /**
         * Check for any timeouts since the last check.
         *
         * @param lastCheck timestamp of the last timeout check
         */
        void checkTimeouts(long lastCheck);
    }

    /**
     * Checks if there is an active subscription for the specified topic.
     *
     * @param topic the topic to check
     * @return true if subscribed to the topic, false otherwise
     */
    boolean isSubscribed(String topic);

    /**
     * Subscribes to a single Kafka topic.
     *
     * @param topic the topic to subscribe to
     */
    void subscribe(String topic);

    /**
     * Subscribes to multiple Kafka topics.
     *
     * @param topicsToAdd collection of topics to subscribe to
     */
    void subscribe(Collection<String> topicsToAdd);

    /**
     * Unsubscribes from a Kafka topic.
     *
     * @param topic the topic to unsubscribe from
     */
    void unsubscribe(String topic);

    /**
     * Processes a single Kafka record.
     *
     * @param record the Kafka record to process
     */
    void processRecord(ConsumerRecord<K, V> record);
}
