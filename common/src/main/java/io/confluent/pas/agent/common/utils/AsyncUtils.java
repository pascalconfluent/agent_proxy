package io.confluent.pas.agent.common.utils;

import io.confluent.pas.agent.common.utils.exceptions.AsyncUtilException;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Utility class for executing functions asynchronously.
 * This class provides a method to execute a function in a separate thread and
 * return the result.
 */
@Slf4j
public class AsyncUtils {

    private final static ExecutorService EXECUTOR_SERVICE = Executors.newFixedThreadPool(10);

    /**
     * Executes a function asynchronously and returns the result.
     *
     * @param request  the request object to be passed to the function
     * @param function the function to be executed
     * @param <R>      the type of the request object
     * @param <T>      the type of the result object
     * @return the result of the function execution
     */
    public static <R, T> T executeFunction(R request, Function<R, T> function) {
        try {
            return EXECUTOR_SERVICE.submit(() -> function.apply(request)).get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error executing async function: {}", e.getMessage());
            throw new AsyncUtilException("Error executing async function", e);
        }
    }


    /**
     * Executes a consumer function asynchronously with the given request object.
     * This method submits the consumer to the executor service and waits for its completion.
     *
     * @param request  the request object to be passed to the consumer
     * @param consumer the consumer function to be executed
     * @param <R>      the type of the request object
     * @throws AsyncUtilException if the execution is interrupted or fails
     */
    public static <R> void executeConsumer(R request, Consumer<R> consumer) {
        try {
            EXECUTOR_SERVICE.submit(() -> consumer.accept(request)).get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error executing async function: {}", e.getMessage());
            throw new AsyncUtilException("Error executing async function", e);
        }
    }


}
