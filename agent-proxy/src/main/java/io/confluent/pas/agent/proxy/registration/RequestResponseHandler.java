package io.confluent.pas.agent.proxy.registration;

import com.fasterxml.jackson.databind.JsonNode;
import io.confluent.pas.agent.common.services.KafkaConfiguration;
import io.confluent.pas.agent.common.services.schemas.Registration;
import io.confluent.pas.agent.proxy.frameworks.java.models.Key;
import io.confluent.pas.agent.proxy.registration.kafka.ProducerService;
import io.confluent.pas.agent.proxy.registration.kafka.ConsumerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Collection;

/**
 * Handle requests and responses
 */
@Slf4j
@Component
public class RequestResponseHandler implements DisposableBean {

    private final ProducerService producerService;
    private final ConsumerService consumerService;

    @Autowired
    public RequestResponseHandler(KafkaConfiguration kafkaConfiguration,
                                  @Value("${kafka.response.timeout:10000}") long responseTimeout) {
        this(new ProducerService(kafkaConfiguration),
                new ConsumerService(kafkaConfiguration, responseTimeout));
    }

    public RequestResponseHandler(ProducerService producerService,
                                  ConsumerService consumerService) {
        this.producerService = producerService;
        this.consumerService = consumerService;
    }

    public void addRegistrations(Collection<Registration> registrations) {
        consumerService.addRegistrations(registrations);
    }

    public void registerHandler(Registration registration,
                                String correlationId,
                                ConsumerService.ResponseHandler handler,
                                ConsumerService.ErrorHandler errorHandler) {
        consumerService.registerResponseHandler(
                registration,
                correlationId,
                handler,
                errorHandler);
    }

    public void unregisterHandler(Registration registration, String correlationId) {
        consumerService.unregisterResponseHandler(registration, correlationId);
    }

    public Mono<Void> sendRequest(Registration registration,
                                  Key key,
                                  JsonNode request) {
        return producerService.send(registration.getRequestTopicName(), key, request);
    }

    @Override
    public void destroy() throws Exception {
        consumerService.close();
        producerService.close();
    }
}
