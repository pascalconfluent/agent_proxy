package io.confluent.pas.agent.common.services.cache;

import com.github.benmanes.caffeine.cache.*;
import io.kcache.CacheLoader;
import io.kcache.utils.InMemoryCache;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.Comparator;
import java.util.Map;

/**
 * A local caching implementation that extends InMemoryCache with Caffeine caching capabilities.
 * This cache supports maximum size limits, expiration policies, and optional loading functionality.
 *
 * @param <K> The type of keys maintained by this cache
 * @param <V> The type of mapped values
 */
public class LocalCache<K, V> extends InMemoryCache<K, V> {
    private final Cache<K, V> cache;
    /**
     * The loader used to load values when they are not present in the cache.
     * Can be null if no loading functionality is required.
     */
    private final CacheLoader<K, V> loader;

    /**
     * Constructs a LocalCache with a specified comparator for key ordering.
     *
     * @param comparator the comparator to determine the ordering of keys
     */
    public LocalCache(Comparator<? super K> comparator) {
        this(null, null, null, comparator);
    }

    /**
     * Constructs a LocalCache with size limits, expiration policy, and loading functionality.
     *
     * @param maximumSize      the maximum number of entries the cache may contain
     * @param expireAfterWrite the time after which entries should be automatically removed
     * @param loader           the cache loader to use when entries are not found
     */
    public LocalCache(Integer maximumSize, Duration expireAfterWrite, CacheLoader<K, V> loader) {
        this.loader = loader;
        this.cache = this.createCache(maximumSize, expireAfterWrite);
    }

    /**
     * Constructs a LocalCache with size limits, expiration policy, loading functionality, and key ordering.
     *
     * @param maximumSize      the maximum number of entries the cache may contain
     * @param expireAfterWrite the time after which entries should be automatically removed
     * @param loader           the cache loader to use when entries are not found
     * @param comparator       the comparator to determine the ordering of keys
     */
    public LocalCache(Integer maximumSize, Duration expireAfterWrite, CacheLoader<K, V> loader, Comparator<? super K> comparator) {
        super(comparator);
        this.loader = loader;
        this.cache = this.createCache(maximumSize, expireAfterWrite);
    }

    /**
     * Returns true if this cache contains a mapping for the specified key.
     *
     * @param key key whose presence in this cache is to be tested
     * @return true if this cache contains a mapping for the specified key
     */
    public boolean containsKey(Object key) {
        return this.get(key) != null;
    }

    /**
     * Returns the value associated with the specified key, or null if there is no cached value.
     * If a loader is configured and the key isn't present, it will attempt to load the value.
     *
     * @param key the key whose associated value is to be returned
     * @return the value associated with the specified key, or null if none
     */
    @SuppressWarnings("unchecked")
    public V get(Object key) {
        return (cache instanceof LoadingCache<K, V> loadingCache)
                ? loadingCache.get((K) key)
                : super.get(key);
    }

    /**
     * Associates the specified value with the specified key in this cache.
     *
     * @param key   key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     * @return the previous value associated with key, or null if there was no mapping
     */
    public V put(K key, V value) {
        V originalValue = this.get(key);

        this.cache.put(key, value);
        delegate().put(key, value);

        return originalValue;
    }

    /**
     * Copies all entries from the specified map to this cache.
     *
     * @param entries mappings to be stored in this cache
     */
    public void putAll(@NotNull Map<? extends K, ? extends V> entries) {
        this.cache.putAll(entries);
        for (Map.Entry<? extends K, ? extends V> entry : entries.entrySet()) {
            delegate().put(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Removes the entry for the specified key if present.
     *
     * @param key key whose mapping is to be removed from the cache
     * @return the previous value associated with key, or null if there was no mapping
     */
    @SuppressWarnings("unchecked")
    public V remove(Object key) {
        V originalValue = this.get(key);
        if (key != null) {
            this.cache.invalidate((K) key);
        }

        return originalValue;
    }

    /**
     * Removes all entries from this cache.
     */
    public void clear() {
        this.cache.invalidateAll();
    }

    /**
     * Creates a new Caffeine cache instance with the specified configuration.
     *
     * @param maximumSize      the maximum number of entries the cache may contain
     * @param expireAfterWrite the time after which entries should be automatically removed
     * @return a new configured Cache instance
     */
    private Cache<K, V> createCache(Integer maximumSize, Duration expireAfterWrite) {
        Caffeine<K, V> caffeine = Caffeine
                .newBuilder()
                .evictionListener((key, value, cause) -> this.delegate().remove(key, value));

        if (maximumSize != null && maximumSize >= 0) {
            caffeine.maximumSize((long) maximumSize);
        }

        if (expireAfterWrite != null && !expireAfterWrite.isNegative()) {
            caffeine.scheduler(Scheduler.systemScheduler()).expireAfterWrite(expireAfterWrite);
        }

        if (this.loader != null) {
            return caffeine.build((key) -> {
                V value = this.loader.load(key);
                if (value != null) {
                    this.delegate().put(key, value);
                }

                return value;
            });
        }

        return caffeine.build();
    }
}
