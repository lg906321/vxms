package org.jacpfx.vertx.websocket.response;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import org.jacpfx.common.CustomSupplier;
import org.jacpfx.common.exceptions.EndpointExecutionException;
import org.jacpfx.vertx.websocket.decoder.Decoder;
import org.jacpfx.vertx.websocket.encoder.Encoder;
import org.jacpfx.vertx.websocket.registry.WebSocketEndpoint;
import org.jacpfx.vertx.websocket.registry.WebSocketRegistry;

import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Created by Andy Moncsek on 17.11.15.
 */
public class WebSocketHandler {
    private final static int cpuCount = Runtime.getRuntime().availableProcessors();
    private final static ExecutorService EXECUTOR = Executors.newFixedThreadPool(cpuCount <= 2 ? cpuCount : cpuCount % 2 == 0 ? cpuCount / 2 : cpuCount);  // TODO use instance count!!
    private final WebSocketEndpoint endpoint;
    private final Vertx vertx;
    private final WebSocketRegistry registry;
    private byte[] value;


    public WebSocketHandler(WebSocketRegistry registry, WebSocketEndpoint endpoint, byte[] value, Vertx vertx) {
        this.endpoint = endpoint;
        this.vertx = vertx;
        this.registry = registry;
        this.value = value;
    }

    /**
     * Returns the Endpoint definition with URL and handler ids
     *
     * @return {@see WSEndpoint}
     */
    public WebSocketEndpoint endpoint() {
        return this.endpoint;
    }

    /**
     * Returns the payload handler which gives you access to the payload transmitted
     *
     * @return {@see Payload}
     */
    public Payload payload() {
        return new Payload(value);
    }


    public TargetType response() {
        return new TargetType(endpoint, vertx, false);
    }

    public class Payload {
        private final byte[] value;

        private Payload(byte[] value) {
            this.value = value;
        }

        public Optional<String> getString() {
            return Optional.ofNullable(value != null ? new String(value) : null);
        }

        public Optional<byte[]> getBytes() {
            return Optional.ofNullable(value);
        }


        public <T> Optional<T> getObject(Class<T> clazz, Decoder decoder) {
            Objects.requireNonNull(clazz);
            Objects.requireNonNull(decoder);
            try {
                if (decoder instanceof Decoder.ByteDecoder) {
                    return ((Decoder.ByteDecoder<T>) decoder).decode(value);
                } else if (decoder instanceof Decoder.StringDecoder) {
                    return ((Decoder.StringDecoder<T>) decoder).decode(new String(value));
                }
            } catch (ClassCastException e) {
                return Optional.empty();
            } catch (Exception e) {
                e.printStackTrace();
            }

            return Optional.empty();
        }
    }

    public class TargetType {
        private final WebSocketEndpoint endpoint;
        private final Vertx vertx;
        private final boolean async;

        private TargetType(WebSocketEndpoint endpoint, Vertx vertx, boolean async) {
            this.endpoint = endpoint;
            this.vertx = vertx;
            this.async = async;
        }

        public TargetType async() {
            return new TargetType(endpoint, vertx, true);
        }

        public ResponseType toAll() {
            return new ResponseType(new WebSocketEndpoint[]{endpoint}, vertx, async, CommType.ALL);
        }

        public ResponseType toAllBut(WebSocketEndpoint... endpoint) {
            // TODO iteration over stream / filter
            return new ResponseType(endpoint, vertx, async, CommType.ALL_BUT_CALLER);
        }

        public ResponseType toCaller() {
            return new ResponseType(new WebSocketEndpoint[]{endpoint}, vertx, async, CommType.CALLER);
        }

        public ResponseType to(WebSocketEndpoint... endpoint) {
            return new ResponseType(endpoint, vertx, async, CommType.TO);
        }


    }

    public class ResponseType {
        private final WebSocketEndpoint[] endpoint;
        private final Vertx vertx;
        private final boolean async;
        private final CommType commType;

        private ResponseType(WebSocketEndpoint[] endpoint, Vertx vertx, final boolean async, final CommType commType) {
            this.endpoint = endpoint;
            this.vertx = vertx;
            this.async = async;
            this.commType = commType;
        }

        public ExecuteWSResponse byteResponse(CustomSupplier<byte[]> byteSupplier) {
            return new ExecuteWSResponse(endpoint, vertx, async, commType, byteSupplier, null, null, null, null, null, null, null, 0, 0L);
        }

        public ExecuteWSResponse stringResponse(CustomSupplier<String> stringSupplier) {
            return new ExecuteWSResponse(endpoint, vertx, async, commType, null, stringSupplier, null, null, null, null, null, null, 0, 0L);
        }

        public ExecuteWSResponse objectResponse(CustomSupplier<Serializable> objectSupplier, Encoder encoder) {
            return new ExecuteWSResponse(endpoint, vertx, async, commType, null, null, objectSupplier, encoder, null, null, null, null, 0, 0L);
        }
    }

    public class ExecuteWSResponse {
        private final WebSocketEndpoint[] endpoint;
        private final Vertx vertx;
        private final boolean async;
        private final CommType commType;
        private final CustomSupplier<byte[]> byteSupplier;
        private final CustomSupplier<String> stringSupplier;
        private final CustomSupplier<Serializable> objectSupplier;
        private final Encoder encoder;
        private final Consumer<Throwable> errorHandler; // TODO replace by function?
        private final Function<Throwable, byte[]> errorHandlerByte; // TODO replace by function?
        private final Function<Throwable, String> errorHandlerString; // TODO replace by function?
        private final Function<Throwable, Serializable> errorHandlerObject; // TODO replace by function?
        private final int retryCount;
        private final long timeout;

        private ExecuteWSResponse(WebSocketEndpoint[] endpoint, Vertx vertx, boolean async, CommType commType, CustomSupplier<byte[]> byteSupplier, CustomSupplier<String> stringSupplier, CustomSupplier<Serializable> objectSupplier, Encoder encoder, Consumer<Throwable> errorHandler, Function<Throwable, byte[]> errorHandlerByte, Function<Throwable, String> errorHandlerString, Function<Throwable, Serializable> errorHandlerObject, int retryCount, long timeout) {
            this.endpoint = endpoint;
            this.vertx = vertx;
            this.async = async;
            this.commType = commType;
            this.byteSupplier = byteSupplier;
            this.stringSupplier = stringSupplier;
            this.objectSupplier = objectSupplier;
            this.encoder = encoder;
            this.errorHandler = errorHandler;
            this.errorHandlerByte = errorHandlerByte;
            this.errorHandlerString = errorHandlerString;
            this.errorHandlerObject = errorHandlerObject;
            this.retryCount = retryCount;
            this.timeout = timeout;
        }

        public ExecuteWSResponse onError(Consumer<Throwable> errorHandler) {
            return new ExecuteWSResponse(endpoint, vertx, async, commType, byteSupplier, stringSupplier, objectSupplier, encoder, errorHandler, errorHandlerByte, errorHandlerString, errorHandlerObject, retryCount, timeout);
        }

        public ExecuteWSResponse onByteResponseError(Function<Throwable, byte[]> errorHandlerByte) {
            return new ExecuteWSResponse(endpoint, vertx, async, commType, byteSupplier, stringSupplier, objectSupplier, encoder, errorHandler, errorHandlerByte, errorHandlerString, errorHandlerObject, retryCount, timeout);
        }

        public ExecuteWSResponse onStringResponseError(Function<Throwable, String> errorHandlerString) {
            return new ExecuteWSResponse(endpoint, vertx, async, commType, byteSupplier, stringSupplier, objectSupplier, encoder, errorHandler, errorHandlerByte, errorHandlerString, errorHandlerObject, retryCount, timeout);
        }

        public ExecuteWSResponse onObjectResponseError(Function<Throwable, Serializable> errorHandlerObject) {
            return new ExecuteWSResponse(endpoint, vertx, async, commType, byteSupplier, stringSupplier, objectSupplier, encoder, errorHandler, errorHandlerByte, errorHandlerString, errorHandlerObject, retryCount, timeout);
        }


        public ExecuteWSResponse retry(int retryCount) {
            return new ExecuteWSResponse(endpoint, vertx, async, commType, byteSupplier, stringSupplier, objectSupplier, encoder, errorHandler, errorHandlerByte, errorHandlerString, errorHandlerObject, retryCount, timeout);
        }

        // TODO implement delay, but throw exception when not in async mode!
        public ExecuteWSResponse delay(int retryCount) {
            return new ExecuteWSResponse(endpoint, vertx, async, commType, byteSupplier, stringSupplier, objectSupplier, encoder, errorHandler, errorHandlerByte, errorHandlerString, errorHandlerObject, retryCount, timeout);
        }

        public ExecuteWSResponse timeout(long timeout) {
            return new ExecuteWSResponse(endpoint, vertx, async, commType, byteSupplier, stringSupplier, objectSupplier, encoder, errorHandler, errorHandlerByte, errorHandlerString, errorHandlerObject, retryCount, timeout);
        }

        public void execute() {
            if (async) {
                Optional.ofNullable(byteSupplier).
                        ifPresent(supplier -> CompletableFuture.supplyAsync(() -> {
                            byte[] result = new byte[0];
                            int retry = retryCount > 0 ? retryCount : 0;
                            while (retry >= 0) {

                                try {
                                    result = supplier.get();
                                    retry = -1;
                                } catch (Throwable e) {
                                    retry--;
                                    if (retry < 0) {
                                        if (errorHandler != null) {
                                            errorHandler.accept(e);
                                        }
                                        if (errorHandlerByte != null) {
                                            result = errorHandlerByte.apply(e);
                                        }
                                        if(errorHandler==null && errorHandlerByte==null) {
                                            throw new EndpointExecutionException(e);
                                        }
                                    }
                                }
                            }

                            return result;
                        }, EXECUTOR).thenAccept(this::sendBinary));
                Optional.ofNullable(stringSupplier).
                        ifPresent(supplier -> CompletableFuture.supplyAsync(() -> {
                            String result = "";
                            int retry = retryCount > 0 ? retryCount : 0;
                            while (retry >= 0) {

                                try {
                                    result = supplier.get();
                                    retry = -1;
                                } catch (Throwable e) {
                                    retry--;
                                    if (retry < 0) {
                                        if (errorHandler != null) {
                                            errorHandler.accept(e);
                                        }
                                        if (errorHandlerString != null) {
                                            result = errorHandlerString.apply(e);
                                        }
                                        if(errorHandler==null && errorHandlerString==null) {
                                            throw new EndpointExecutionException(e);
                                        }
                                    }
                                }
                            }
                            return result;
                        }, EXECUTOR).thenAccept(this::sendText));
                Optional.ofNullable(objectSupplier).
                        ifPresent(supplier -> CompletableFuture.supplyAsync(() -> {
                            Serializable result = "";
                            int retry = retryCount > 0 ? retryCount : 0;
                            while (retry >= 0) {

                                try {
                                    result = supplier.get();
                                    retry = -1;
                                } catch (Throwable e) {
                                    retry--;
                                    if (retry < 0) {
                                        if (errorHandler != null) {
                                            errorHandler.accept(e);
                                        }
                                        if (errorHandlerObject != null) {
                                            result = errorHandlerObject.apply(e);
                                        }
                                        if(errorHandler==null && errorHandlerObject==null) {
                                            throw new EndpointExecutionException(e);
                                        }
                                    }
                                }
                            }
                            return result;
                        }, EXECUTOR).thenAccept(value -> serialize(value, encoder).ifPresent(val -> {
                            if (val instanceof String) {
                                sendText((String) val);
                            } else {
                                sendBinary((byte[]) val);
                            }
                        })));
            } else {
                // TODO check for exception, think about @OnError method execution
                Optional.ofNullable(byteSupplier).
                        ifPresent(supplier -> {
                            byte[] result = null;
                            int retry = retryCount > 0 ? retryCount : 0;
                            while (retry >= 0) {

                                try {
                                    result = supplier.get();
                                    retry = -1;
                                } catch (Throwable e) {
                                    retry--;
                                    if (retry < 0) {
                                        if (errorHandler != null) {
                                            errorHandler.accept(e);
                                        }
                                        if (errorHandlerByte != null) {
                                            result = errorHandlerByte.apply(e);
                                        }
                                        if(errorHandler==null && errorHandlerByte==null) {
                                            throw new EndpointExecutionException(e);
                                        }
                                    }
                                }
                            }

                            Optional.ofNullable(result).ifPresent(this::sendBinary);
                        });
                // TODO check for exception, think about @OnError method execution
                Optional.ofNullable(stringSupplier).
                        ifPresent(supplier -> {
                            String result = null;
                            int retry = retryCount > 0 ? retryCount : 0;
                            while (retry >= 0) {

                                try {
                                    result = supplier.get();
                                    retry = -1;
                                } catch (Throwable e) {
                                    retry--;
                                    if (retry < 0) {
                                        if (errorHandler != null) {
                                            errorHandler.accept(e);
                                        }
                                        if (errorHandlerString != null) {
                                            result = errorHandlerString.apply(e);
                                        }
                                        if(errorHandler==null && errorHandlerString==null) {
                                            throw new EndpointExecutionException(e);
                                        }
                                    }
                                }
                            }

                            Optional.ofNullable(result).ifPresent(this::sendText);
                        });
                // TODO check for exception, think about @OnError method execution

                Optional.ofNullable(objectSupplier).
                        ifPresent(supplier -> {
                            Serializable result = null;
                            int retry = retryCount > 0 ? retryCount : 0;
                            while (retry >= 0) {

                                try {
                                    result = supplier.get();
                                    retry = -1;
                                } catch (Throwable e) {
                                    retry--;
                                    if (retry < 0) {
                                        if (errorHandler != null) {
                                            errorHandler.accept(e);
                                        }
                                        if (errorHandlerObject != null) {
                                            result = errorHandlerObject.apply(e);
                                        }
                                        if(errorHandler==null && errorHandlerObject==null) {
                                            throw new EndpointExecutionException(e);
                                        }
                                    }

                                }
                            }

                            Optional.ofNullable(result).ifPresent(value -> serialize(value, encoder).ifPresent(val -> {
                                if (val instanceof String) {
                                    sendText((String) val);
                                } else {
                                    sendBinary((byte[]) val);
                                }
                            }));
                        });
            }
        }


        // TODO handle exceptions
        private Optional<?> serialize(Serializable value, Encoder encoder) {
            if (encoder instanceof Encoder.ByteEncoder) {
                return Optional.ofNullable(((Encoder.ByteEncoder) encoder).encode(value));
            } else if (encoder instanceof Encoder.StringEncoder) {
                return Optional.ofNullable(((Encoder.StringEncoder) encoder).encode(value));
            }

            return Optional.empty();
        }

        private void sendText(String value) {
            switch (commType) {

                case ALL:
                    registry.findEndpointsAndExecute(endpoint[0], match -> vertx.eventBus().send(match.getTextHandlerId(), value));
                    break;
                case ALL_BUT_CALLER:
                    registry.findEndpointsAndExecute(endpoint[0], match -> {
                        if (!endpoint[0].equals(match)) vertx.eventBus().send(match.getTextHandlerId(), value);
                    });
                    break;
                case CALLER:
                    vertx.eventBus().send(endpoint[0].getTextHandlerId(), value);
                    break;
                case TO:
                    Stream.of(endpoint).forEach(ep -> vertx.eventBus().send(ep.getTextHandlerId(), value));
                    break;
            }
        }

        private void sendBinary(byte[] value) {
            switch (commType) {

                case ALL:
                    registry.findEndpointsAndExecute(endpoint[0], match -> vertx.eventBus().send(match.getBinaryHandlerId(), Buffer.buffer(value)));
                    break;
                case ALL_BUT_CALLER:
                    registry.findEndpointsAndExecute(endpoint[0], match -> {
                        if (!endpoint[0].equals(match))
                            vertx.eventBus().send(match.getTextHandlerId(), Buffer.buffer(value));
                    });
                    break;
                case CALLER:
                    vertx.eventBus().send(endpoint[0].getBinaryHandlerId(), Buffer.buffer(value));
                    break;
                case TO:
                    Stream.of(endpoint).forEach(ep -> vertx.eventBus().send(ep.getBinaryHandlerId(), Buffer.buffer(value)));
                    break;
            }
        }
    }

    public enum CommType {
        ALL, ALL_BUT_CALLER, CALLER, TO
    }
}