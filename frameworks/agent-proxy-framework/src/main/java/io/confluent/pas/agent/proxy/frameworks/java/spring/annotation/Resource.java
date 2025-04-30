package io.confluent.pas.agent.proxy.frameworks.java.spring.annotation;

import io.confluent.pas.agent.common.services.schemas.ResourceResponse;
import io.confluent.pas.agent.proxy.frameworks.java.models.Key;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used to mark methods that expose resources in a Spring application.
 * Resources are endpoints that handle file or data requests through Kafka-based messaging.
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Resource {

    /**
     * The unique name identifier for this resource.
     */
    String name();

    /**
     * A description of what this resource provides.
     */
    String description();

    /**
     * The Kafka topic name where resource requests will be received.
     */
    String request_topic() default "";

    /**
     * The Kafka topic name where resource responses will be sent.
     */
    String response_topic() default "";

    /**
     * The MIME type of the resource content.
     */
    String contentType();

    /**
     * The endpoint path where this resource will be exposed.
     */
    String path();

    /**
     * The class type for the response message payload.
     * Must extend ResourceResponse.
     */
    Class<? extends ResourceResponse> responseClass();
}