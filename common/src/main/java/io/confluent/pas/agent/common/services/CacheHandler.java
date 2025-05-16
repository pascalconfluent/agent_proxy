package io.confluent.pas.agent.common.services;

import io.kcache.CacheUpdateHandler;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.TopicPartition;

import java.util.HashMap;
import java.util.Map;


/**
 * Handles cache updates and initialization for a Kafka-based caching system.
 * This class implements CacheUpdateHandler to manage cache events and updates,
 * providing a way to accumulate and process cache entries during initialization.
 *
 * @param <K> The type of keys in the cache
 * @param <V> The type of values in the cache
 */
@Slf4j
public class CacheHandler<K, V> implements CacheUpdateHandler<K, V> {

    /**
     * Interface defining the contract for handling cache values.
     * Implementations process batches of key-value pairs from the cache.
     *
     * @param <K> The type of keys in the cache
     * @param <V> The type of values in the cache
     */
    public interface Handler<K, V> {
        /**
         * Processes a batch of cache entries.
         *
         * @param registrations Map of key-value pairs to process
         */
        void onValues(Map<K, V> registrations);
    }

    /**
     * Handler for processing cache values
     */
    private final Handler<K, V> handler;
    /**
     * Temporary storage for cache entries during initialization
     */
    private final Map<K, V> accumulator = new HashMap<>();
    /**
     * Flag indicating if the cache has been initialized
     */
    private boolean initialized = false;

    /**
     * Flag indicating if the cache is empty after initialization
     */
    @Getter
    private boolean empty;

    public CacheHandler(Handler<K, V> handler) {
        this.handler = handler;
    }

    /**
     * Called when the cache is initialized.
     * Processes accumulated entries if any exist, or marks the cache as empty if no entries were found.
     *
     * @param count       Number of entries in the cache
     * @param checkpoints Map of topic partitions and their offsets
     */
    @Override
    public void cacheInitialized(int count, Map<TopicPartition, Long> checkpoints) {
        initialized = true;

        if (count == 0) {
            // No event, we might need to register the schemas
            empty = true;
        } else if (!accumulator.isEmpty()) {
            handler.onValues(accumulator);
            accumulator.clear();
        }

        log.info("Registration cache initialized.");
    }

    /**
     * Handles updates to the cache entries.
     * If the cache is initialized, processes updates immediately.
     * Otherwise, accumulates updates for later processing.
     *
     * @param key      Key being updated
     * @param value    New value
     * @param oldValue Previous value
     * @param tp       Topic partition
     * @param offset   Kafka offset
     * @param ts       Timestamp of the update
     */
    @Override
    public void handleUpdate(K key,
                             V value,
                             V oldValue,
                             TopicPartition tp,
                             long offset,
                             long ts) {
        // If the cache is not initialized, store the value in the accumulator
        // This will help to keep the last event for each key
        if (initialized) {
            handler.onValues(Map.of(key, value));
        } else {
            if (value == null) {
                accumulator.remove(key);
            } else {
                accumulator.put(key, value);
            }
        }
    }
}