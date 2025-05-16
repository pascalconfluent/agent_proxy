package io.confluent.pas.agent.common.services;

import io.confluent.kafka.serializers.json.KafkaJsonSchemaDeserializer;
import io.confluent.kafka.serializers.json.KafkaJsonSchemaDeserializerConfig;
import io.confluent.kafka.serializers.json.KafkaJsonSchemaSerializer;
import io.confluent.kafka.serializers.json.KafkaJsonSchemaSerializerConfig;
import io.confluent.pas.agent.common.services.cache.LocalCache;
import io.kcache.CacheUpdateHandler;
import io.kcache.KafkaCache;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * A generic cache implementation that wraps KafkaCache to provide distributed caching capabilities.
 * This cache uses Kafka as a backing store and supports JSON schema serialization/deserialization.
 *
 * @param <K> The type of keys in the cache
 * @param <V> The type of values in the cache
 */
public class Cache<K, V> implements Map<K, V> {

    private final KafkaCache<K, V> cache;

    /**
     * Creates a new Cache instance with the specified configuration.
     *
     * @param kafkaConfiguration The Kafka configuration settings
     * @param cacheName          The name of the cache
     * @param kClass             The class type of the keys
     * @param vClass             The class type of the values
     * @param handler            The handler for cache updates
     * @param readOnly           Whether the cache is read-only
     * @param topicName          The name of the Kafka topic to use
     */
    public Cache(KafkaConfiguration kafkaConfiguration,
                 String cacheName,
                 Class<K> kClass,
                 Class<V> vClass,
                 CacheHandler.Handler<K, V> handler,
                 boolean readOnly,
                 String topicName) {
        this.cache = initialize(
                kafkaConfiguration,
                cacheName,
                kClass,
                vClass,
                handler,
                readOnly,
                topicName);
    }

    public Cache(KafkaCache<K, V> cache) {
        this.cache = cache;
    }

    @Override
    public int size() {
        return cache.size();
    }

    @Override
    public boolean isEmpty() {
        return cache.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return cache.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return cache.containsValue(value);
    }

    @Override
    public V get(Object key) {
        return cache.get(key);
    }

    @Override
    public @Nullable V put(K key, V value) {
        return cache.put(key, value);
    }

    @Override
    public V remove(Object key) {
        return cache.remove(key);
    }

    @Override
    public void putAll(@NotNull Map<? extends K, ? extends V> m) {
        cache.putAll(m);
    }

    @Override
    public void clear() {
        cache.clear();
    }

    @Override
    public @NotNull Set<K> keySet() {
        return cache.keySet();
    }

    @Override
    public @NotNull Collection<V> values() {
        return cache.values();
    }

    @Override
    public @NotNull Set<Entry<K, V>> entrySet() {
        return cache.entrySet();
    }

    /**
     * Initializes the KafkaCache with the specified configuration.
     * Sets up JSON schema serialization/deserialization and configures the cache storage.
     *
     * @param kafkaConfiguration The Kafka configuration settings
     * @param cacheName          The name of the cache
     * @param kClass             The class type of the keys
     * @param vClass             The class type of the values
     * @param handler            The handler for cache updates
     * @param readOnly           Whether the cache is read-only
     * @param topicName          The name of the Kafka topic to use
     * @return The initialized KafkaCache instance
     */
    private KafkaCache<K, V> initialize(KafkaConfiguration kafkaConfiguration,
                                        String cacheName,
                                        Class<K> kClass,
                                        Class<V> vClass,
                                        CacheHandler.Handler<K, V> handler,
                                        boolean readOnly,
                                        String topicName) {
        // Retrieve the Schema Registry configuration settings from KafkaPropertiesFactory.
        final Map<String, Object> srConfig = KafkaPropertiesFactory.getSchemaRegistryConfig(kafkaConfiguration);
        srConfig.put(KafkaJsonSchemaDeserializerConfig.JSON_KEY_TYPE, kClass);
        srConfig.put(KafkaJsonSchemaDeserializerConfig.JSON_VALUE_TYPE, vClass);
        srConfig.put(KafkaJsonSchemaSerializerConfig.AUTO_REGISTER_SCHEMAS, true);

        // Create and configure the Serde for the registration key using Kafka JSON schema serializer and deserializer.
        final Serde<K> keySerdes = new Serdes.WrapperSerde<>(
                new KafkaJsonSchemaSerializer<>(),
                new KafkaJsonSchemaDeserializer<>()
        );
        keySerdes.configure(srConfig, true);

        // Create and configure the Serde for the registration value using Kafka JSON schema serializer and deserializer.
        final Serde<V> valueSerdes = new Serdes.WrapperSerde<>(
                new KafkaJsonSchemaSerializer<>(),
                new KafkaJsonSchemaDeserializer<>()
        );
        valueSerdes.configure(srConfig, false);

        final CacheUpdateHandler<K, V> serviceHandler = handler != null
                ? new CacheHandler<>(handler)
                : null;

        final KafkaCache<K, V> cache = new KafkaCache<>(
                KafkaPropertiesFactory.getCacheConfig(
                        kafkaConfiguration,
                        readOnly,
                        topicName,
                        cacheName),
                keySerdes,
                valueSerdes,
                serviceHandler,
                new LocalCache<>(
                        1000,
                        Duration.of(5, ChronoUnit.MINUTES),
                        null)
        );

        cache.init();

        return cache;
    }
}
