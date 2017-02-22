package org.jacpfx.vertx.event.response.blocking;

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import org.jacpfx.common.ThrowableFunction;
import org.jacpfx.common.ThrowableSupplier;
import org.jacpfx.vertx.event.interfaces.blocking.ExecuteEventbusStringCallBlocking;

import java.util.function.Consumer;

/**
 * Created by Andy Moncsek on 12.01.16.
 */
public class ExecuteEventbusStringResponse extends ExecuteEventbusString {


    public ExecuteEventbusStringResponse(String methodId,
                                         Vertx vertx,
                                         Throwable t,
                                         Consumer<Throwable> errorMethodHandler,
                                         Message<Object> message,
                                         ThrowableSupplier<String> stringSupplier,
                                         ExecuteEventbusStringCallBlocking excecuteAsyncEventBusAndReply,
                                         Consumer<Throwable> errorHandler,
                                         ThrowableFunction<Throwable, String> onFailureRespond,
                                         DeliveryOptions deliveryOptions,
                                         int retryCount, long timeout,
                                         long delay, long circuitBreakerTimeout) {
        super(methodId, vertx, t,
                errorMethodHandler,
                message,
                stringSupplier,
                excecuteAsyncEventBusAndReply,
                errorHandler,
                onFailureRespond,
                deliveryOptions,
                retryCount,
                timeout,
                delay, circuitBreakerTimeout);
    }


    /**
     * defines an action for errors in byte responses, you can handle the error and return an alternate createResponse value
     *
     * @param onFailureRespond the handler (function) to execute on error
     * @return the createResponse chain
     */
    public ExecuteEventbusString onFailureRespond(ThrowableFunction<Throwable, String> onFailureRespond) {
        return new ExecuteEventbusString(methodId, vertx, t, errorMethodHandler, message, stringSupplier, excecuteAsyncEventBusAndReply, errorHandler,
                onFailureRespond, deliveryOptions, retryCount, timeout, delay, circuitBreakerTimeout);
    }


    public ExecuteEventbusStringResponse onError(Consumer<Throwable> errorHandler) {
        return new ExecuteEventbusStringResponse(methodId, vertx, t, errorMethodHandler, message, stringSupplier, excecuteAsyncEventBusAndReply, errorHandler,
                onFailureRespond, deliveryOptions, retryCount, timeout, delay, circuitBreakerTimeout);
    }

    /**
     * retry operation on error
     *
     * @param retryCount
     * @return the createResponse chain
     */
    public ExecuteEventbusStringCircuitBreaker retry(int retryCount) {
        return new ExecuteEventbusStringCircuitBreaker(methodId, vertx, t, errorMethodHandler, message, stringSupplier, excecuteAsyncEventBusAndReply, errorHandler,
                onFailureRespond, deliveryOptions, retryCount, timeout, delay, circuitBreakerTimeout);
    }

    /**
     * Defines how long a method can be executed before aborted.
     *
     * @param timeout time to wait in ms
     * @return the createResponse chain
     */
    public ExecuteEventbusStringResponse timeout(long timeout) {
        return new ExecuteEventbusStringResponse(methodId, vertx, t, errorMethodHandler, message, stringSupplier, excecuteAsyncEventBusAndReply, errorHandler,
                onFailureRespond, deliveryOptions, retryCount, timeout, delay, circuitBreakerTimeout);
    }

    /**
     * Defines the delay (in ms) between the createResponse retries (on error).
     *
     * @param delay
     * @return the createResponse chain
     */
    public ExecuteEventbusStringResponse delay(long delay) {
        return new ExecuteEventbusStringResponse(methodId, vertx, t, errorMethodHandler, message, stringSupplier, excecuteAsyncEventBusAndReply, errorHandler,
                onFailureRespond, deliveryOptions, retryCount, timeout, delay, circuitBreakerTimeout);
    }


}