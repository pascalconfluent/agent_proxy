package io.confluent.pas.agent.proxy.frameworks.java.spring.annotation;

import io.confluent.pas.agent.common.services.KafkaConfiguration;
import io.confluent.pas.agent.common.services.schemas.Registration;
import io.confluent.pas.agent.common.services.schemas.ResourceRegistration;
import io.confluent.pas.agent.common.services.schemas.ResourceRequest;
import io.confluent.pas.agent.proxy.frameworks.java.SubscriptionHandler;
import io.confluent.pas.agent.proxy.frameworks.java.spring.annotation.exceptions.AgentInitializationException;
import io.confluent.pas.agent.proxy.frameworks.java.spring.annotation.exceptions.AgentInvocationException;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.context.ApplicationContext;

import java.io.Closeable;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Automatically registers and manages agent methods annotated with @Agent or @Resource in a Spring application.
 * This class acts as a bridge between Spring beans containing agent/resource methods and the Kafka-based messaging system.
 * It scans the application context for beans and their methods, setting up subscription handlers for
 * methods annotated with @Agent or @Resource, ensuring that messages can be handled appropriately.
 */
@Slf4j
@AutoConfiguration
@AutoConfigureOrder
public class AgentRegistrar implements InitializingBean, Closeable {
    private static final String SELF_BEAN_NAME = AgentRegistrar.class.getSimpleName();

    /**
     * Supplier for creating SubscriptionHandlers
     */
    public interface SubscriptionHandlerSupplier {
        SubscriptionHandler<?, ?> get(Class<?> requestClass, Class<?> responseClass);
    }

    /**
     * A record representing an invocation handler for a subscription.
     * It contains a MethodHandle for the annotated method and a SubscriptionHandler for managing subscriptions.
     *
     * @param method              The MethodHandle for the method to be invoked.
     * @param subscriptionHandler The SubscriptionHandler responsible for subscribing to messages.
     */
    @Builder
    record InvocationHandler(MethodHandle method,
                             SubscriptionHandler<?, ?> subscriptionHandler) implements Closeable {

        /**
         * Subscribes the handler to the specified registration.
         *
         * @param registration The registration information for the subscription.
         * @return The current InvocationHandler instance.
         */
        public InvocationHandler subscribe(Registration registration) {
            subscriptionHandler.subscribeWith(
                    registration,
                    (request) -> {
                        try {
                            method.invoke(request);
                        } catch (Throwable e) {
                            log.error("Failed to invoke handler method via MethodHandles", e);
                            throw new AgentInvocationException("Failed to invoke handler method", e);
                        }
                    });

            return this;
        }

        @Override
        public void close() {
            if (subscriptionHandler != null) {
                subscriptionHandler.close();
            }
        }
    }

    /**
     * Spring application context for accessing beans
     */
    private final ApplicationContext applicationContext;

    /**
     * List to keep track of all active subscription handlers
     */
    private final List<InvocationHandler> handlers = new ArrayList<>();

    /**
     * Supplier for creating SubscriptionHandlers
     */
    private final SubscriptionHandlerSupplier subscriptionHandlerSupplier;

    /**
     * Creates a new AgentRegistrar with the required dependencies.
     *
     * @param kafkaConfiguration Configuration for Kafka messaging
     * @param applicationContext Spring application context
     */
    @Autowired
    public AgentRegistrar(KafkaConfiguration kafkaConfiguration,
                          ApplicationContext applicationContext) {
        this(applicationContext,
                (requestClass, responseClass) ->
                        new SubscriptionHandler<>(kafkaConfiguration, requestClass, responseClass)
        );

    }

    public AgentRegistrar(ApplicationContext applicationContext,
                          SubscriptionHandlerSupplier subscriptionHandlerSupplier) {
        this.applicationContext = applicationContext;
        this.subscriptionHandlerSupplier = subscriptionHandlerSupplier;
    }


    /**
     * Initializes the registrar after all properties are set.
     * Scans all Spring beans for methods annotated with @Agent and @Resource and sets up their handlers.
     */
    @Override
    public void afterPropertiesSet() {
        final String[] beanNames = applicationContext.getBeanDefinitionNames();
        for (String beanName : beanNames) {
            // Skip self-registration to avoid infinite loops
            if (beanName.endsWith(SELF_BEAN_NAME)) {
                continue;
            }

            final Object bean = applicationContext.getBean(beanName);

            final Method[] methods = bean.getClass().getMethods();
            // Stream through the methods of the bean to find those annotated with @Agent or @Resource
            Arrays.stream(methods)
                    .filter(m -> m.isAnnotationPresent(Agent.class) || m.isAnnotationPresent(Resource.class))
                    .forEach(method -> {
                        // If the method is annotated with @Agent, handle it accordingly
                        if (method.isAnnotationPresent(Agent.class)) {
                            final Agent agent = method.getAnnotation(Agent.class);
                            if (agent == null) {
                                log.error("Failed to find @Agent annotation on method {}", method.getName());
                                throw new AgentInitializationException("Failed to find @Agent annotation");
                            }

                            final InvocationHandler info = getSubscriptionHandler(method, agent, bean);
                            // Track the handler for cleanup
                            addHandler(info);
                        }
                        // If the method is annotated with @Resource, handle it accordingly
                        else if (method.isAnnotationPresent(Resource.class)) {
                            final Resource resource = method.getAnnotation(Resource.class);
                            if (resource == null) {
                                log.error("Failed to find @Resource annotation on method {}", method.getName());
                                throw new AgentInitializationException("Failed to find @Resource annotation");
                            }

                            final InvocationHandler info = getSubscriptionHandler(method, resource, bean);
                            // Track the handler for cleanup
                            addHandler(info);
                        }
                    });
        }
    }

    /**
     * Adds a handler to the list of active handlers.
     *
     * @param handler The handler to add.
     */
    void addHandler(InvocationHandler handler) {
        handlers.add(handler);
    }

    /**
     * Creates a subscription handler for the given method and bean using the Resource annotation.
     *
     * @param method   Method annotated with @Resource
     * @param resource Resource annotation
     * @param bean     Bean containing the method
     * @return Subscription handler for the method
     */
    @NotNull
    private InvocationHandler getSubscriptionHandler(Method method, Resource resource, Object bean) {
        log.info("Found resource {} on method {}", resource.name(), method.getName());

        String requestTopic = getTopicName(resource.name(), resource.request_topic(), "-request");
        String responseTopic = getTopicName(resource.name(), resource.response_topic(), "-response");

        // Create registration info for the resource
        final ResourceRegistration registration = new ResourceRegistration(
                resource.name(),
                resource.description(),
                requestTopic,
                responseTopic,
                resource.contentType(),
                resource.path());

        // Create and start a subscription handler for the resource
        SubscriptionHandler<?, ?> subscriptionHandler = subscriptionHandlerSupplier.get(
                ResourceRequest.class,
                resource.responseClass());

        return getInvocationHandler(method, bean, registration, subscriptionHandler);
    }

    /**
     * Creates a subscription handler for the given method and bean using the Agent annotation.
     *
     * @param method Method annotated with @Agent
     * @param agent  Agent annotation
     * @param bean   Bean containing the method
     * @return Subscription handler for the method
     */
    @NotNull
    private InvocationHandler getSubscriptionHandler(Method method, Agent agent, Object bean) {
        log.info("Found agent {} on method {}", agent.name(), method.getName());

        // Define topic names if not provided
        String requestTopic = getTopicName(agent.name(), agent.request_topic(), "-request");
        String responseTopic = getTopicName(agent.name(), agent.response_topic(), "-response");

        // Create registration info for the agent
        final Registration registration = new Registration(
                agent.name(),
                agent.description(),
                requestTopic,
                responseTopic);

        // Create and start a subscription handler for the agent
        SubscriptionHandler<?, ?> subscriptionHandler = subscriptionHandlerSupplier.get(
                agent.requestClass(),
                agent.responseClass());

        return getInvocationHandler(method, bean, registration, subscriptionHandler);
    }

    /**
     * Creates an invocation handler for the given method and bean using the provided registration and subscription handler.
     *
     * @param method              Method annotated with @Agent or @Resource
     * @param bean                Bean containing the method
     * @param registration        Registration information for the method
     * @param subscriptionHandler Subscription handler for the method
     * @return Invocation handler for the method
     */
    @NotNull
    private InvocationHandler getInvocationHandler(Method method, Object bean, Registration registration, SubscriptionHandler<?, ?> subscriptionHandler) {
        try {
            // Create a MethodHandle for the method to allow dynamic invocation
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            MethodHandle methodHandle = lookup.unreflect(method).bindTo(bean);

            // Set up the message handling by invoking the annotated method
            return InvocationHandler.builder()
                    .subscriptionHandler(subscriptionHandler)
                    .method(methodHandle)
                    .build()
                    .subscribe(registration);
        } catch (IllegalAccessException e) {
            log.error("Failed to create MethodHandle for method {}", method.getName(), e);
            throw new AgentInvocationException("Failed to create MethodHandle", e);
        }
    }

    /**
     * Generates a topic name based on the provided name, topic, and suffix.
     * If the topic is empty, it appends the suffix to the name.
     *
     * @param name   The base name
     * @param topic  The topic name
     * @param suffix The suffix to append if the topic is empty
     * @return The generated topic name
     */
    private static String getTopicName(String name, String topic, String suffix) {
        if (StringUtils.isEmpty(topic)) {
            return name + suffix;
        }
        return topic;
    }

    /**
     * Cleans up resources by stopping all subscription handlers.
     */
    @Override
    public void close() {
        handlers.forEach(InvocationHandler::close);
        handlers.clear();
    }
}