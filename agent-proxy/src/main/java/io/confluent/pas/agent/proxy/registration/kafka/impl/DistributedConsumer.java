package io.confluent.pas.agent.proxy.registration.kafka.impl;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.confluent.pas.agent.common.services.KafkaConfiguration;
import io.confluent.pas.agent.proxy.registration.kafka.ConsumerHandler;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.io.Closeable;

public class DistributedConsumer<K, V> extends ConsumerImpl<K, V> implements Closeable {

    private final Cache<String, Pair<K, V>> cache = Caffeine.newBuilder()
            .maximumSize(10000)
            .expireAfterWrite(5, java.util.concurrent.TimeUnit.MINUTES)
            .build();

    public DistributedConsumer(KafkaConfiguration kafkaConfiguration, Class<V> requestClass, ConsumerHandler<K, V> consumerHandler, TimeoutChecker timeoutChecker) {
        super(kafkaConfiguration, requestClass, consumerHandler, timeoutChecker);
    }

    public DistributedConsumer(ConsumerHandler<K, V> consumerHandler, KafkaConsumer<K, V> kafkaConsumer, TimeoutChecker timeoutChecker) {
        super(consumerHandler, kafkaConsumer, timeoutChecker);
    }


}
