package org.jacpfx.vertx.event.response;

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import org.jacpfx.common.ThrowableSupplier;
import org.jacpfx.common.encoder.Encoder;
import org.jacpfx.vertx.event.response.blocking.ExecuteEventbusByteResponse;
import org.jacpfx.vertx.event.response.blocking.ExecuteEventbusObjectResponse;
import org.jacpfx.vertx.event.response.blocking.ExecuteEventbusStringResponse;

import java.io.Serializable;
import java.util.function.Consumer;

/**
 * Created by Andy Moncsek on 12.01.16.
 * Fluent API to define a Task and to reply the request with the output of your task.
 */
public class EventbusResponseBlocking {
    private final String methodId;
    private final Vertx vertx;
    private final Throwable failure;
    private final Consumer<Throwable> errorMethodHandler;
    private final Message<Object> message;

    /**
     * The constructor to pass all needed members
     *
     * @param methodId           the method identifier
     * @param message            the event-bus message to respond to
     * @param vertx              the vertx instance
     * @param failure            the failure thrown while task execution
     * @param errorMethodHandler the error handler
     */
    public EventbusResponseBlocking(String methodId, Message<Object> message,Vertx vertx, Throwable failure, Consumer<Throwable> errorMethodHandler) {
        this.methodId = methodId;
        this.vertx = vertx;
        this.failure = failure;
        this.errorMethodHandler = errorMethodHandler;
        this.message = message;

    }


    /**
     * Retunrs a byte array to the target type
     *
     * @param byteSupplier supplier which returns the createResponse value as byte array
     * @return {@link ExecuteEventbusByteResponse}
     */
    public ExecuteEventbusByteResponse byteResponse(ThrowableSupplier<byte[]> byteSupplier) {
        return new ExecuteEventbusByteResponse(methodId, vertx, failure, errorMethodHandler, message, byteSupplier, null, null, null, null, 0, 0l, 0l, 0l);
    }

    /**
     * Retunrs a String to the target type
     *
     * @param stringSupplier supplier which returns the createResponse value as String
     * @return {@link ExecuteEventbusStringResponse}
     */
    public ExecuteEventbusStringResponse stringResponse(ThrowableSupplier<String> stringSupplier) {
        return new ExecuteEventbusStringResponse(methodId, vertx, failure, errorMethodHandler, message, stringSupplier, null, null, null, null, 0, 0l, 0l, 0l);
    }

    /**
     * Retunrs a Serializable to the target type
     *
     * @param objectSupplier supplier which returns the createResponse value as Serializable
     * @return {@link ExecuteEventbusObjectResponse}
     */
    public ExecuteEventbusObjectResponse objectResponse(ThrowableSupplier<Serializable> objectSupplier, Encoder encoder) {
        return new ExecuteEventbusObjectResponse(methodId, vertx, failure, errorMethodHandler, message, objectSupplier, null, encoder, null, null, null, 0, 0, 0l, 0l);
    }
}