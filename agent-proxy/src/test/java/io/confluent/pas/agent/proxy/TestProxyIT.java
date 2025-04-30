package io.confluent.pas.agent.proxy;

import io.confluent.pas.agent.common.services.KafkaConfiguration;
import io.confluent.pas.agent.common.services.schemas.Registration;
import io.confluent.pas.agent.proxy.frameworks.java.SubscriptionHandler;
import io.confluent.pas.agent.proxy.registration.RegistrationCoordinator;
import io.confluent.pas.agent.proxy.registration.handlers.CompositeHandler;
import io.confluent.pas.agent.proxy.registration.handlers.RegistrationHandler;
import io.confluent.pas.agent.proxy.registration.RequestResponseHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;


@EnableAsync
@Testcontainers
@SpringBootTest
public class TestProxyIT {

    private static final String KAFKA_IMAGE = "apache/kafka:3.7.0";

    private final CountDownLatch latch = new CountDownLatch(1);

    @Container
    private static final KafkaContainer kafka = new KafkaContainer(KAFKA_IMAGE)
            .withStartupTimeout(Duration.ofMinutes(2))
            .withStartupAttempts(3);

    @DynamicPropertySource
    public static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("kafka.broker-servers", kafka::getBootstrapServers);
    }

    private final KafkaConfiguration kafkaConfiguration;
    private final RegistrationCoordinator coordinator;
    private final ProxyEventListener listener;

    @Autowired
    public TestProxyIT(KafkaConfiguration kafkaConfiguration,
                       RegistrationCoordinator coordinator,
                       ProxyEventListener listener) {
        this.kafkaConfiguration = kafkaConfiguration;
        this.coordinator = coordinator;
        this.listener = listener;
    }

    @Test
    public void testToolRegistration() throws InterruptedException {
        listener.reset();

        final SubscriptionHandler<String, String> handler = new SubscriptionHandler<>(
                kafkaConfiguration,
                String.class,
                String.class);

        Registration registration = new Registration(
                "test",
                "Sample registration",
                "sample_req",
                "sample_res");

        handler.subscribeWith(registration, (req) -> {
            System.out.println("Received registration");
        });

        listener.waitForEvent();

        final List<CompositeHandler> handlers = coordinator.getAllRegistrationHandlers();
        assertThat(handlers).isNotNull();
        assertThat(handlers.size()).isEqualTo(1);
    }
}
