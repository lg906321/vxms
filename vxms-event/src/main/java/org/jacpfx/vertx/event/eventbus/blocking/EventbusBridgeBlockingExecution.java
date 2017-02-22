package org.jacpfx.vertx.event.eventbus.blocking;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.shareddata.Counter;
import io.vertx.core.shareddata.Lock;
import io.vertx.core.shareddata.SharedData;
import org.jacpfx.common.ThrowableFunction;
import org.jacpfx.common.ThrowableSupplier;
import org.jacpfx.common.encoder.Encoder;
import org.jacpfx.vertx.event.interfaces.blocking.RecursiveBlockingExecutor;
import org.jacpfx.vertx.event.interfaces.blocking.RetryBlockingExecutor;
import org.jacpfx.vertx.event.response.basic.ResponseExecution;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * Created by Andy Moncsek on 05.04.16.
 * Handles event-bus call and blocking execution of the message to create an event-bus response
 */
public class EventbusBridgeBlockingExecution {

    public static final long LOCK_VALUE = -1l;
    public static final int DEFAULT_LOCK_TIMEOUT = 2000;
    public static final long NO_TIMEOUT = 0l;


    /**
     * Send event-bus message and process the result in the passed function for blocking execution chain
     * @param methodId the method identifier
     * @param targetId the event-bus target id
     * @param message the message to send
     * @param function the function to process the result message
     * @param deliveryOptions the event-bus delivery options
     * @param vertx  the vertx instance
     * @param errorMethodHandler the error-method handler
     * @param requestMessage the request message to respond after chain execution
     * @param encoder the encoder to serialize the response object
     * @param errorHandler the error handler
     * @param onFailureRespond the function that takes a Future with the alternate response value in case of failure
     * @param responseDeliveryOptions the delivery options for the event response
     * @param retryCount the amount of retries before failure execution is triggered
     * @param timeout the amount of time before the execution will be aborted
     * @param delay the delay time in ms between an execution error and the retry
     * @param circuitBreakerTimeout the amount of time before the circuit breaker closed again
     * @param executor the typed executor to process the chain
     * @param retryExecutor the typed retry executor of the chain
     * @param <T>  the type of response
     */
    public static <T> void sendMessageAndSupplyHandler(String methodId,
                                                       String targetId,
                                                       Object message,
                                                       ThrowableFunction<AsyncResult<Message<Object>>, T> function,
                                                       DeliveryOptions deliveryOptions,
                                                       Vertx vertx,
                                                       Consumer<Throwable> errorMethodHandler,
                                                       Message<Object> requestMessage,
                                                       Encoder encoder,
                                                       Consumer<Throwable> errorHandler,
                                                       ThrowableFunction<Throwable, T> onFailureRespond,
                                                       DeliveryOptions responseDeliveryOptions,
                                                       int retryCount, long timeout, long delay, long circuitBreakerTimeout,
                                                       RecursiveBlockingExecutor executor, RetryBlockingExecutor retryExecutor) {

        if (circuitBreakerTimeout == 0l) {
            executeDefaultState(targetId,
                    message,
                    function,
                    deliveryOptions,
                    methodId,
                    vertx,
                    errorMethodHandler,
                    requestMessage,
                    encoder,
                    errorHandler,
                    onFailureRespond,
                    responseDeliveryOptions,
                    retryCount, timeout, delay, circuitBreakerTimeout, executor, retryExecutor, null);
        } else {
            executeStateful(targetId,
                    message,
                    function,
                    deliveryOptions,
                    methodId,
                    vertx,
                    errorMethodHandler,
                    requestMessage,
                    encoder,
                    errorHandler,
                    onFailureRespond,
                    responseDeliveryOptions,
                    retryCount, timeout, delay, circuitBreakerTimeout, executor, retryExecutor);
        }
    }

    private static <T> void executeStateful(String targetId,
                                            Object message,
                                            ThrowableFunction<AsyncResult<Message<Object>>, T> byteFunction,
                                            DeliveryOptions deliveryOptions,
                                            String methodId,
                                            Vertx vertx,
                                            Consumer<Throwable> errorMethodHandler,
                                            Message<Object> requestMessage,
                                            Encoder encoder,
                                            Consumer<Throwable> errorHandler,
                                            ThrowableFunction<Throwable, T> onFailureRespond,
                                            DeliveryOptions responseDeliveryOptions,
                                            int retryCount, long timeout, long delay, long circuitBreakerTimeout,
                                            RecursiveBlockingExecutor executor, RetryBlockingExecutor retry) {

        executeLocked(((lock, counter) ->
                counter.get(counterHandler -> {
                    long currentVal = counterHandler.result();
                    if (currentVal == 0) {
                        executeInitialState(targetId,
                                message,
                                byteFunction,
                                deliveryOptions,
                                methodId,
                                vertx,
                                errorMethodHandler,
                                requestMessage,
                                encoder,
                                errorHandler,
                                onFailureRespond,
                                responseDeliveryOptions,
                                retryCount, timeout, delay,
                                circuitBreakerTimeout, executor,
                                retry, lock, counter);
                    } else if (currentVal > 0) {
                        executeDefaultState(targetId,
                                message,
                                byteFunction,
                                deliveryOptions,
                                methodId,
                                vertx,
                                errorMethodHandler,
                                requestMessage,
                                encoder,
                                errorHandler,
                                onFailureRespond,
                                responseDeliveryOptions,
                                retryCount, timeout, delay,
                                circuitBreakerTimeout, executor, retry, lock);
                    } else {
                        executeErrorState(methodId,
                                vertx,
                                errorMethodHandler,
                                requestMessage,
                                encoder,
                                errorHandler,
                                onFailureRespond,
                                responseDeliveryOptions,
                                retryCount, timeout, delay,
                                circuitBreakerTimeout,executor, lock);
                    }
                })), methodId, vertx, errorMethodHandler, requestMessage, encoder, errorHandler, onFailureRespond, responseDeliveryOptions, retryCount, timeout, delay, circuitBreakerTimeout,executor);
    }


    private static <T> void executeInitialState(String targetId,
                                                Object message,
                                                ThrowableFunction<AsyncResult<Message<Object>>, T> byteFunction,
                                                DeliveryOptions deliveryOptions,
                                                String methodId,
                                                Vertx vertx,
                                                Consumer<Throwable> errorMethodHandler,
                                                Message<Object> requestMessage,
                                                Encoder encoder,
                                                Consumer<Throwable> errorHandler,
                                                ThrowableFunction<Throwable, T> onFailureRespond,
                                                DeliveryOptions responseDeliveryOptions,
                                                int retryCount, long timeout, long delay, long circuitBreakerTimeout,
                                                RecursiveBlockingExecutor executor, RetryBlockingExecutor retry, Lock lock, Counter counter) {
        int incrementCounter = retryCount + 1;
        counter.addAndGet(Integer.valueOf(incrementCounter).longValue(), rHandler ->
                executeDefaultState(targetId,
                        message,
                        byteFunction,
                        deliveryOptions,
                        methodId,
                        vertx,
                        errorMethodHandler,
                        requestMessage,
                        encoder,
                        errorHandler,
                        onFailureRespond,
                        responseDeliveryOptions,
                        retryCount, timeout, delay,
                        circuitBreakerTimeout, executor, retry, lock));
    }

    private static <T> void executeDefaultState(String targetId,
                                                Object message,
                                                ThrowableFunction<AsyncResult<Message<Object>>, T> function,
                                                DeliveryOptions deliveryOptions,
                                                String methodId,
                                                Vertx vertx,
                                                Consumer<Throwable> errorMethodHandler,
                                                Message<Object> requestMessage,
                                                Encoder encoder,
                                                Consumer<Throwable> errorHandler,
                                                ThrowableFunction<Throwable, T> onFailureRespond,
                                                DeliveryOptions responseDeliveryOptions,
                                                int retryCount, long timeout, long delay, long circuitBreakerTimeout,
                                                RecursiveBlockingExecutor executor, RetryBlockingExecutor retry, Lock lock) {

        Optional.ofNullable(lock).ifPresent(Lock::release);
        vertx.
                eventBus().
                send(targetId, message, deliveryOptions,
                        event ->
                                createSupplierAndExecute(targetId,
                                        message,
                                        function,
                                        deliveryOptions,
                                        methodId,
                                        vertx,
                                        errorMethodHandler,
                                        requestMessage,
                                        encoder,
                                        errorHandler,
                                        onFailureRespond,
                                        responseDeliveryOptions,
                                        retryCount, timeout, delay, circuitBreakerTimeout,executor, retry, event));
    }

    private static <T> void executeErrorState(String methodId,
                                              Vertx vertx,
                                              Consumer<Throwable> errorMethodHandler,
                                              Message<Object> requestMessage,
                                              Encoder encoder,
                                              Consumer<Throwable> errorHandler,
                                              ThrowableFunction<Throwable, T> onFailureRespond,
                                              DeliveryOptions responseDeliveryOptions,
                                              int retryCount, long timeout, long delay, long circuitBreakerTimeout,
                                              RecursiveBlockingExecutor executor, Lock lock) {
        final Throwable cause = Future.failedFuture("circuit open").cause();
        handleError(methodId,
                vertx,
                errorMethodHandler,
                requestMessage,
                encoder,
                errorHandler,
                onFailureRespond,
                responseDeliveryOptions,
                retryCount, timeout, delay,
                circuitBreakerTimeout, executor, lock, cause);
    }

    private static <T> void createSupplierAndExecute(String targetId,
                                                     Object message,
                                                     ThrowableFunction<AsyncResult<Message<Object>>, T> function,
                                                     DeliveryOptions deliveryOptions,
                                                     String methodId,
                                                     Vertx vertx,
                                                     Consumer<Throwable> errorMethodHandler,
                                                     Message<Object> requestMessage,
                                                     Encoder encoder,
                                                     Consumer<Throwable> errorHandler,
                                                     ThrowableFunction<Throwable, T> onFailureRespond,
                                                     DeliveryOptions responseDeliveryOptions,
                                                     int retryCount, long timeout, long delay, long circuitBreakerTimeout,
                                                     RecursiveBlockingExecutor executor, RetryBlockingExecutor retry,
                                                     AsyncResult<Message<Object>> event) {
        final ThrowableSupplier<T> supplier = createSupplier(targetId,
                message,
                function,
                deliveryOptions,
                methodId,
                vertx,
                errorMethodHandler,
                requestMessage,
                encoder,
                errorHandler,
                onFailureRespond,
                responseDeliveryOptions,
                retryCount, timeout, delay, circuitBreakerTimeout, retry, event);

        if (circuitBreakerTimeout == NO_TIMEOUT) {
            statelessExecution(targetId,
                    message,
                    function,
                    deliveryOptions,
                    methodId,
                    vertx,
                    errorMethodHandler,
                    requestMessage,
                    encoder,
                    errorHandler,
                    onFailureRespond,
                    responseDeliveryOptions,
                    retryCount, timeout, delay, circuitBreakerTimeout,executor,retry, event, supplier);
        } else {
            statefulExecution(targetId,
                    message,
                    function,
                    deliveryOptions,
                    methodId,
                    vertx,
                    errorMethodHandler,
                    requestMessage,
                    encoder,
                    errorHandler,
                    onFailureRespond,
                    responseDeliveryOptions,
                    retryCount, timeout, delay, circuitBreakerTimeout,executor,retry, event, supplier);
        }
    }

    private static <T> void statelessExecution(String targetId,
                                               Object message,
                                               ThrowableFunction<AsyncResult<Message<Object>>, T> function,
                                               DeliveryOptions deliveryOptions,
                                               String methodId,
                                               Vertx vertx,
                                               Consumer<Throwable> errorMethodHandler,
                                               Message<Object> requestMessage,
                                               Encoder encoder,
                                               Consumer<Throwable> errorHandler,
                                               ThrowableFunction<Throwable, T> onFailureRespond,
                                               DeliveryOptions responseDeliveryOptions,
                                               int retryCount, long timeout, long delay, long circuitBreakerTimeout,
                                               RecursiveBlockingExecutor executor, RetryBlockingExecutor retry,
                                               AsyncResult<Message<Object>> event, ThrowableSupplier<T> byteSupplier) {
        if (event.succeeded() || (event.failed() && retryCount <= 0)) {
            executor.execute(methodId, vertx, event.cause(), errorMethodHandler, requestMessage, byteSupplier, encoder,
                    errorHandler, onFailureRespond, responseDeliveryOptions, retryCount, timeout, delay, circuitBreakerTimeout);
        } else if (event.failed() && retryCount > 0) {
            retryFunction(targetId,
                    message,
                    function,
                    deliveryOptions,
                    methodId, vertx,
                    event.cause(),
                    errorMethodHandler,
                    requestMessage,encoder,
                    errorHandler, onFailureRespond,
                    responseDeliveryOptions,
                    retryCount, timeout,
                    delay, circuitBreakerTimeout,retry);
        }
    }

    private static <T> void statefulExecution(String targetId,
                                              Object message,
                                              ThrowableFunction<AsyncResult<Message<Object>>, T> function,
                                              DeliveryOptions deliveryOptions,
                                              String methodId,
                                              Vertx vertx,
                                              Consumer<Throwable> errorMethodHandler,
                                              Message<Object> requestMessage,
                                              Encoder encoder,
                                              Consumer<Throwable> errorHandler,
                                              ThrowableFunction<Throwable, T> onFailureRespond,
                                              DeliveryOptions responseDeliveryOptions,
                                              int retryCount, long timeout, long delay, long circuitBreakerTimeout,
                                              RecursiveBlockingExecutor executor, RetryBlockingExecutor retry,
                                              AsyncResult<Message<Object>> event, ThrowableSupplier<T> supplier) {
        if (event.succeeded()) {
            executor.execute(methodId, vertx, event.cause(), errorMethodHandler, requestMessage, supplier, encoder,
                    errorHandler, onFailureRespond, responseDeliveryOptions, retryCount, timeout, delay, circuitBreakerTimeout);
        } else {
            statefulErrorHandling(targetId, message, function, deliveryOptions,
                    methodId, vertx, errorMethodHandler, requestMessage, encoder, errorHandler,
                    onFailureRespond, responseDeliveryOptions, retryCount, timeout, delay, circuitBreakerTimeout,executor,retry, event);
        }
    }

    private static <T> void statefulErrorHandling(String targetId,
                                                  Object message,
                                                  ThrowableFunction<AsyncResult<Message<Object>>, T> function,
                                                  DeliveryOptions deliveryOptions,
                                                  String methodId,
                                                  Vertx vertx,
                                                  Consumer<Throwable> errorMethodHandler,
                                                  Message<Object> requestMessage,
                                                  Encoder encoder,
                                                  Consumer<Throwable> errorHandler,
                                                  ThrowableFunction<Throwable, T> onFailureRespond,
                                                  DeliveryOptions responseDeliveryOptions,
                                                  int retryCount, long timeout, long delay, long circuitBreakerTimeout,
                                                  RecursiveBlockingExecutor executor, RetryBlockingExecutor retry, AsyncResult<Message<Object>> event) {

        executeLocked((lock, counter) ->
                counter.decrementAndGet(valHandler -> {
                    if (valHandler.succeeded()) {
                        long count = valHandler.result();
                        if (count <= 0) {
                            openCircuitAndHandleError(methodId, vertx, errorMethodHandler, requestMessage, encoder, errorHandler,
                                    onFailureRespond, responseDeliveryOptions, retryCount, timeout, delay, circuitBreakerTimeout, executor, event, lock, counter);
                        } else {
                            lock.release();
                            final Throwable cause = event.cause();
                            retryFunction(targetId, message, function, deliveryOptions, methodId, vertx, cause, errorMethodHandler, requestMessage, encoder,errorHandler,
                                    onFailureRespond, responseDeliveryOptions, retryCount, timeout, delay, circuitBreakerTimeout, retry);
                        }
                    } else {
                        final Throwable cause = valHandler.cause();
                        handleError(methodId, vertx, errorMethodHandler, requestMessage, encoder,
                                errorHandler, onFailureRespond, responseDeliveryOptions, retryCount, timeout, delay,
                                circuitBreakerTimeout, executor, lock, cause);
                    }
                }), methodId, vertx, errorMethodHandler, requestMessage, encoder, errorHandler, onFailureRespond, responseDeliveryOptions, retryCount, timeout, delay, circuitBreakerTimeout, executor);


    }

    private static <T> void openCircuitAndHandleError(String methodId,
                                                      Vertx vertx,
                                                      Consumer<Throwable> errorMethodHandler,
                                                      Message<Object> requestMessage,
                                                      Encoder encoder,
                                                      Consumer<Throwable> errorHandler,
                                                      ThrowableFunction<Throwable, T> onFailureRespond,
                                                      DeliveryOptions responseDeliveryOptions,
                                                      int retryCount, long timeout, long delay, long circuitBreakerTimeout,
                                                      RecursiveBlockingExecutor executor,
                                                      AsyncResult<Message<Object>> event, Lock lock, Counter counter) {
        resetLockTimer(vertx, retryCount, circuitBreakerTimeout, counter);
        lockAndHandle(counter, val -> {
            final Throwable cause = event.cause();
            handleError(methodId,
                    vertx,
                    errorMethodHandler,
                    requestMessage,
                    encoder,
                    errorHandler,
                    onFailureRespond,
                    responseDeliveryOptions,
                    retryCount,
                    timeout, delay,
                    circuitBreakerTimeout, executor,
                    lock, cause);
        });
    }

    private static void lockAndHandle(Counter counter, Handler<AsyncResult<Long>> asyncResultHandler) {
        counter.addAndGet(LOCK_VALUE, asyncResultHandler);
    }

    private static void resetLockTimer(Vertx vertx, int retryCount, long circuitBreakerTimeout, Counter counter) {
        vertx.setTimer(circuitBreakerTimeout, timer -> counter.addAndGet(Integer.valueOf(retryCount + 1).longValue(), val -> {
        }));
    }

    private static <T> void handleError(String methodId,
                                        Vertx vertx,
                                        Consumer<Throwable> errorMethodHandler,
                                        Message<Object> requestMessage,
                                        Encoder encoder,
                                        Consumer<Throwable> errorHandler,
                                        ThrowableFunction<Throwable, T> onFailureRespond,
                                        DeliveryOptions responseDeliveryOptions,
                                        int retryCount, long timeout, long delay, long circuitBreakerTimeout, RecursiveBlockingExecutor executor, Lock lock, Throwable cause) {
        Optional.ofNullable(lock).ifPresent(Lock::release);
        final ThrowableSupplier<T> failConsumer = () -> {
            assert cause != null;
            throw cause;
        };
        executor.execute(methodId, vertx, cause, errorMethodHandler, requestMessage, failConsumer, encoder,
                errorHandler, onFailureRespond, responseDeliveryOptions, retryCount, timeout, delay, circuitBreakerTimeout);
    }

    private static <T> void retryFunction(String targetId,
                                          Object message,
                                          ThrowableFunction<AsyncResult<Message<Object>>, T> function,
                                          DeliveryOptions requestDeliveryOptions,
                                          String methodId,
                                          Vertx vertx,
                                          Throwable t,
                                          Consumer<Throwable> errorMethodHandler,
                                          Message<Object> requestMessage,
                                          Encoder encoder,
                                          Consumer<Throwable> errorHandler,
                                          ThrowableFunction<Throwable, T> onFailureRespond,
                                          DeliveryOptions responseDeliveryOptions,
                                          int retryCount, long timeout, long delay,
                                          long circuitBreakerTimeout, RetryBlockingExecutor retry) {
        ResponseExecution.handleError(errorHandler, t);
        retry.execute(targetId,
                message,
                function,
                requestDeliveryOptions,
                methodId,
                vertx, t,
                errorMethodHandler,
                requestMessage,
                null,
                encoder,
                errorHandler,
                onFailureRespond,
                responseDeliveryOptions,
                retryCount,
                timeout, delay, circuitBreakerTimeout);
    }


    private static <T> ThrowableSupplier<T> createSupplier(String targetId,
                                                           Object message,
                                                           ThrowableFunction<AsyncResult<Message<Object>>, T> function,
                                                           DeliveryOptions deliveryOptions,
                                                           String methodId,
                                                           Vertx vertx,
                                                           Consumer<Throwable> errorMethodHandler,
                                                           Message<Object> requestMessage,
                                                           Encoder encoder,
                                                           Consumer<Throwable> errorHandler,
                                                           ThrowableFunction<Throwable, T> onFailureRespond,
                                                           DeliveryOptions responseDeliveryOptions,
                                                           int retryCount, long timeout, long delay, long circuitBreakerTimeout,
                                                           RetryBlockingExecutor retry, AsyncResult<Message<Object>> event) {
        return () -> {
            T resp = null;
            if (event.failed()) {
                if (retryCount > 0) {
                    retryFunction(targetId,
                            message,
                            function,
                            deliveryOptions,
                            methodId, vertx,
                            event.cause(),
                            errorMethodHandler,
                            requestMessage,
                            encoder, errorHandler,
                            onFailureRespond,
                            responseDeliveryOptions,
                            retryCount, timeout, delay,
                            circuitBreakerTimeout, retry);
                } else {
                    throw event.cause();
                }
            } else {
                resp = function.apply(event);
            }

            return resp;
        };
    }

    private static <T> void executeLocked(LockedConsumer consumer,
                                          String methodId,
                                          Vertx vertx,
                                          Consumer<Throwable> errorMethodHandler,
                                          Message<Object> requestMessage,
                                          Encoder encoder,
                                          Consumer<Throwable> errorHandler,
                                          ThrowableFunction<Throwable, T> onFailureRespond,
                                          DeliveryOptions responseDeliveryOptions,
                                          int retryCount, long timeout, long delay,
                                          long circuitBreakerTimeout, RecursiveBlockingExecutor executor) {
        final SharedData sharedData = vertx.sharedData();
        sharedData.getLockWithTimeout(methodId, DEFAULT_LOCK_TIMEOUT, lockHandler -> {
            if (lockHandler.succeeded()) {
                final Lock lock = lockHandler.result();
                sharedData.getCounter(methodId, resultHandler -> {
                    if (resultHandler.succeeded()) {
                        consumer.execute(lock, resultHandler.result());
                    } else {
                        final Throwable cause = resultHandler.cause();
                        handleError(methodId,
                                vertx,
                                errorMethodHandler,
                                requestMessage, encoder,
                                errorHandler,
                                onFailureRespond,
                                responseDeliveryOptions,
                                retryCount, timeout, delay,
                                circuitBreakerTimeout,
                                executor, lock, cause);
                    }
                });
            } else {
                final Throwable cause = lockHandler.cause();
                handleError(methodId,
                        vertx,
                        errorMethodHandler,
                        requestMessage, encoder,
                        errorHandler,
                        onFailureRespond,
                        responseDeliveryOptions,
                        retryCount, timeout, delay,
                        circuitBreakerTimeout, executor,
                        null, cause);
            }

        });
    }


    private interface LockedConsumer {
        void execute(Lock lock, Counter counter);
    }


}