/*
 * Copyright [2017] [Andy Moncsek]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jacpfx.evbbridge;


import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.test.core.VertxTestBase;
import io.vertx.test.fakecluster.FakeClusterManager;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import org.jacpfx.entity.Payload;
import org.jacpfx.vxms.common.ServiceEndpoint;
import org.jacpfx.vxms.common.util.Serializer;
import org.jacpfx.vxms.rest.annotation.OnRestError;
import org.jacpfx.vxms.rest.response.RestHandler;
import org.jacpfx.vxms.services.VxmsEndpoint;
import org.junit.Before;
import org.junit.Test;

/**
 * Created by Andy Moncsek on 23.04.15.
 */
public class RESTJerseyClientEventByteCircuitBreakerAsyncTest extends VertxTestBase {

  public static final String SERVICE_REST_GET = "/wsService";
  public static final int PORT = 9998;
  public static final int PORT2 = 9999;
  public static final int PORT3 = 9991;
  private final static int MAX_RESPONSE_ELEMENTS = 4;
  private static final String HOST = "127.0.0.1";
  private HttpClient client;

  protected int getNumNodes() {
    return 1;
  }

  protected Vertx getVertx() {
    return vertices[0];
  }

  @Override
  protected ClusterManager getClusterManager() {
    return new FakeClusterManager();
  }

  @Override
  public void setUp() throws Exception {
    super.setUp();
    startNodes(getNumNodes());

  }

  @Before
  public void startVerticles() throws InterruptedException {

    CountDownLatch latch2 = new CountDownLatch(3);
    DeploymentOptions options = new DeploymentOptions().setInstances(1);
    options.setConfig(new JsonObject().put("clustered", false).put("host", HOST));
    // Deploy the module - the System property `vertx.modulename` will contain the name of the module so you
    // don'failure have to hardecode it in your tests

    getVertx().deployVerticle(new WsServiceTwo(), options, asyncResult -> {
      // Deployment is asynchronous and this this handler will be called when it's complete (or failed)
      System.out.println("start service: " + asyncResult.succeeded());
      assertTrue(asyncResult.succeeded());
      assertNotNull("deploymentID should not be null", asyncResult.result());
      // If deployed correctly then start the tests!
      //   latch2.countDown();

      latch2.countDown();

    });
    getVertx().deployVerticle(new TestVerticle(), options, asyncResult -> {
      // Deployment is asynchronous and this this handler will be called when it's complete (or failed)
      System.out.println("start service: " + asyncResult.succeeded());
      assertTrue(asyncResult.succeeded());
      assertNotNull("deploymentID should not be null", asyncResult.result());
      // If deployed correctly then start the tests!
      //   latch2.countDown();

      latch2.countDown();

    });
    getVertx().deployVerticle(new TestErrorVerticle(), options, asyncResult -> {
      // Deployment is asynchronous and this this handler will be called when it's complete (or failed)
      System.out.println("start service: " + asyncResult.succeeded());
      assertTrue(asyncResult.succeeded());
      assertNotNull("deploymentID should not be null", asyncResult.result());
      // If deployed correctly then start the tests!
      //   latch2.countDown();

      latch2.countDown();

    });

    client = getVertx().
        createHttpClient(new HttpClientOptions());
    awaitLatch(latch2);

  }


  @Test

  public void simpleSyncNoConnectionErrorResponseTest() throws InterruptedException {
    System.setProperty("sun.net.http.allowRestrictedHeaders", "true");
    CountDownLatch latch = new CountDownLatch(1);
    Client client = ClientBuilder.newClient();
    WebTarget target = client.target("http://" + HOST + ":" + PORT2)
        .path("/wsService/simpleSyncNoConnectionErrorResponse");
    Future<byte[]> getCallback = target.request(MediaType.APPLICATION_JSON_TYPE).async()
        .get(new InvocationCallback<byte[]>() {

          @Override
          public void completed(byte[] response) {
            System.out.println("Response entity '" + response + "' received.");

            vertx.runOnContext(h -> {
              Payload<String> pp = null;
              try {
                pp = (Payload<String>) Serializer.deserialize(response);
              } catch (IOException e) {
                e.printStackTrace();
              } catch (ClassNotFoundException e) {
                e.printStackTrace();
              }
              assertEquals(pp.getValue(), "No handlers for address hello1");
            });
            latch.countDown();
          }

          @Override
          public void failed(Throwable throwable) {
            throwable.printStackTrace();
          }
        });

    latch.await();
    testComplete();

  }


  @Test

  public void simpleSyncNoConnectionAndExceptionErrorResponse() throws InterruptedException {
    System.setProperty("sun.net.http.allowRestrictedHeaders", "true");
    CountDownLatch latch = new CountDownLatch(1);
    Client client = ClientBuilder.newClient();
    WebTarget target = client.target("http://" + HOST + ":" + PORT2)
        .path("/wsService/simpleSyncNoConnectionAndExceptionErrorResponse");
    Future<byte[]> getCallback = target.request(MediaType.APPLICATION_JSON_TYPE).async()
        .get(new InvocationCallback<byte[]>() {

          @Override
          public void completed(byte[] response) {
            System.out.println("Response entity '" + response + "' received.");

            vertx.runOnContext(h -> {
              Payload<String> pp = null;
              try {
                pp = (Payload<String>) Serializer.deserialize(response);
              } catch (IOException e) {
                e.printStackTrace();
              } catch (ClassNotFoundException e) {
                e.printStackTrace();
              }
              assertEquals("fallback response nullpointer in onFailureRespond", pp.getValue());
            });
            latch.countDown();
          }

          @Override
          public void failed(Throwable throwable) {
            throwable.printStackTrace();
          }
        });

    latch.await();
    testComplete();

  }


  @Test

  public void simpleSyncNoConnectionAndExceptionErrorResponseStateful()
      throws InterruptedException {
    System.setProperty("sun.net.http.allowRestrictedHeaders", "true");
    CountDownLatch latch = new CountDownLatch(1);
    Client client = ClientBuilder.newClient();
    WebTarget target = client.target("http://" + HOST + ":" + PORT2)
        .path("/wsService/simpleSyncNoConnectionAndExceptionErrorResponse");
    Future<byte[]> getCallback = target.request(MediaType.APPLICATION_JSON_TYPE).async()
        .get(new InvocationCallback<byte[]>() {

          @Override
          public void completed(byte[] response) {
            System.out.println("Response entity '" + response + "' received.");

            vertx.runOnContext(h -> {
              Payload<String> pp = null;
              try {
                pp = (Payload<String>) Serializer.deserialize(response);
              } catch (IOException e) {
                e.printStackTrace();
              } catch (ClassNotFoundException e) {
                e.printStackTrace();
              }
              assertEquals("fallback response nullpointer in onFailureRespond", pp.getValue());
            });
            latch.countDown();
          }

          @Override
          public void failed(Throwable throwable) {
            throwable.printStackTrace();
          }
        });

    latch.await();
    testComplete();

  }

  @Test

  public void simpleSyncNoConnectionAndExceptionErrorResponseFail() throws InterruptedException {
    System.setProperty("sun.net.http.allowRestrictedHeaders", "true");
    CountDownLatch latch = new CountDownLatch(1);
    Client client = ClientBuilder.newClient();
    WebTarget target = client.target("http://" + HOST + ":" + PORT2)
        .path("/wsService/simpleSyncNoConnectionAndExceptionErrorResponseFail");
    Future<byte[]> getCallback = target.request(MediaType.APPLICATION_JSON_TYPE).async()
        .get(new InvocationCallback<byte[]>() {

          @Override
          public void completed(byte[] response) {

          }

          @Override
          public void failed(Throwable throwable) {

            vertx.runOnContext(h -> {
              assertEquals(
                  "javax.ws.rs.InternalServerErrorException: HTTP 500 nullpointer in onFailureRespond",
                  throwable.getMessage());
            });
            latch.countDown();
            throwable.printStackTrace();
          }
        });

    latch.await();
    testComplete();

  }


  @Test

  public void simpleSyncNoConnectionAndExceptionErrorResponseStatefulFail()
      throws InterruptedException {
    System.setProperty("sun.net.http.allowRestrictedHeaders", "true");
    CountDownLatch latch = new CountDownLatch(1);
    Client client = ClientBuilder.newClient();
    WebTarget target = client.target("http://" + HOST + ":" + PORT2)
        .path("/wsService/simpleSyncNoConnectionAndExceptionErrorResponseStatefulFail");
    Future<byte[]> getCallback = target.request(MediaType.APPLICATION_JSON_TYPE).async()
        .get(new InvocationCallback<byte[]>() {

          @Override
          public void completed(byte[] response) {

          }

          @Override
          public void failed(Throwable throwable) {

            vertx.runOnContext(h -> {
              assertEquals(
                  "javax.ws.rs.InternalServerErrorException: HTTP 500 nullpointer in onFailureRespond",
                  throwable.getMessage());
            });
            latch.countDown();
            throwable.printStackTrace();
          }
        });

    latch.await();
    testComplete();

  }

  @Test

  public void simpleSyncNoConnectionErrorResponseStateful() throws InterruptedException {
    System.setProperty("sun.net.http.allowRestrictedHeaders", "true");
    CountDownLatch latch = new CountDownLatch(1);
    Client client = ClientBuilder.newClient();
    WebTarget target = client.target("http://" + HOST + ":" + PORT2)
        .path("/wsService/simpleSyncNoConnectionErrorResponseStateful");
    Future<byte[]> getCallback = target.request(MediaType.APPLICATION_JSON_TYPE).async()
        .get(new InvocationCallback<byte[]>() {

          @Override
          public void completed(byte[] response) {
            System.out.println("Response entity '" + response + "' received.");

            vertx.runOnContext(h -> {
              Payload<String> pp = null;
              try {
                pp = (Payload<String>) Serializer.deserialize(response);
              } catch (IOException e) {
                e.printStackTrace();
              } catch (ClassNotFoundException e) {
                e.printStackTrace();
              }
              assertEquals(pp.getValue(), "No handlers for address hello1");

              vertx.setTimer(1000, val -> {
                WebTarget target = client.target("http://" + HOST + ":" + PORT2)
                    .path("/wsService/simpleSyncNoConnectionErrorResponseStateful");
                Future<byte[]> getCallback = target.request(MediaType.APPLICATION_JSON_TYPE).async()
                    .get(new InvocationCallback<byte[]>() {

                      @Override
                      public void completed(byte[] response) {
                        System.out.println("Response entity '" + response + "' received.");

                        vertx.runOnContext(h -> {
                          Payload<String> pp = null;
                          try {
                            pp = (Payload<String>) Serializer.deserialize(response);
                          } catch (IOException e) {
                            e.printStackTrace();
                          } catch (ClassNotFoundException e) {
                            e.printStackTrace();
                          }
                          assertEquals(pp.getValue(), "circuit open");

                          vertx.setTimer(2000, val -> {
                            WebTarget target = client.target("http://" + HOST + ":" + PORT2)
                                .path("/wsService/simpleSyncNoConnectionErrorResponseStateful");
                            Future<byte[]> getCallback = target
                                .request(MediaType.APPLICATION_JSON_TYPE).async()
                                .get(new InvocationCallback<byte[]>() {

                                  @Override
                                  public void completed(byte[] response) {
                                    System.out
                                        .println("Response entity '" + response + "' received.");

                                    vertx.runOnContext(h -> {
                                      Payload<String> pp = null;
                                      try {
                                        pp = (Payload<String>) Serializer.deserialize(response);
                                      } catch (IOException e) {
                                        e.printStackTrace();
                                      } catch (ClassNotFoundException e) {
                                        e.printStackTrace();
                                      }
                                      assertEquals(pp.getValue(), "No handlers for address hello1");
                                    });
                                    latch.countDown();
                                  }

                                  @Override
                                  public void failed(Throwable throwable) {
                                    throwable.printStackTrace();
                                  }
                                });
                          });
                        });


                      }

                      @Override
                      public void failed(Throwable throwable) {
                        throwable.printStackTrace();
                      }
                    });
              });
            });


          }

          @Override
          public void failed(Throwable throwable) {
            throwable.printStackTrace();
          }
        });

    latch.await();
    testComplete();

  }


  public HttpClient getClient() {
    return client;
  }


  @ServiceEndpoint(name = SERVICE_REST_GET, contextRoot = SERVICE_REST_GET, port = PORT2)
  public class WsServiceTwo extends VxmsEndpoint {


    @Path("/simpleSyncNoConnectionErrorResponse")
    @GET
    public void simpleSyncNoConnectionErrorResponse(RestHandler reply) {
      System.out.println("-------1");
      reply.eventBusRequest().
          blocking().
          send("hello1", "welt").
          mapToByteResponse((handler) -> {
            System.out.println("value from event  ");
            return Serializer.serialize(new Payload<>(handler.result().body().toString()));
          }).
          onError(error -> {
            System.out.println(":::" + error.getMessage());
          }).
          retry(3).
          onFailureRespond((t) -> Serializer.serialize(new Payload<>(t.getMessage()))).
          execute();
      System.out.println("-------2");
    }

    @Path("/simpleSyncNoConnectionAndExceptionErrorResponse")
    @GET
    public void simpleSyncNoConnectionAndExceptionErrorResponse(RestHandler reply) {
      System.out.println("-------1");
      reply.eventBusRequest().
          blocking().
          send("hello1", "welt").
          mapToByteResponse((handler) -> {
            System.out.println("value from event  ");
            return Serializer.serialize(new Payload<>(handler.result().body().toString()));
          }).
          onError(error -> {
            System.out.println(":::" + error.getMessage());
          }).
          retry(3).
          onFailureRespond((t) -> {
            throw new NullPointerException("nullpointer in onFailureRespond");
          }).
          execute();
      System.out.println("-------2");
    }

    @OnRestError("/simpleSyncNoConnectionAndExceptionErrorResponse")
    @GET
    public void simpleSyncNoConnectionAndExceptionErrorResponseOnFail(RestHandler reply,
        Throwable tt) {
      System.out.println("-------1");
      reply.response().
          blocking().
          byteResponse(() -> {
            System.out.println("value from event  ");
            return Serializer.serialize(new Payload<>("fallback response " + tt.getMessage()));
          }).
          onError(error -> {
            System.out.println(":::" + error.getMessage());
          }).
          retry(3).
          onFailureRespond((t) -> Serializer.serialize(new Payload<>(t.getMessage()))).
          execute();
      System.out.println("-------2");
    }

    @Path("/simpleSyncNoConnectionAndExceptionErrorResponseFail")
    @GET
    public void simpleSyncNoConnectionAndExceptionErrorResponseFail(RestHandler reply) {
      System.out.println("-------1");
      reply.eventBusRequest().
          blocking().
          send("hello1", "welt").
          mapToByteResponse((handler) -> {
            System.out.println("value from event  ");
            return Serializer.serialize(new Payload<>(handler.result().body().toString()));
          }).
          onError(error -> {
            System.out.println(":::" + error.getMessage());
          }).
          retry(3).
          onFailureRespond((t) -> {
            throw new NullPointerException("nullpointer in onFailureRespond");
          }).
          execute();
      System.out.println("-------2");
    }

    @Path("/simpleSyncNoConnectionErrorResponseStateful")
    @GET
    public void simpleSyncNoConnectionErrorResponseStateful(RestHandler reply) {
      System.out.println("-------1");
      reply.eventBusRequest().
          blocking().
          send("hello1", "welt").
          mapToByteResponse((handler) -> {
            System.out.println("value from event  ");
            return Serializer.serialize(new Payload<>(handler.result().body().toString()));
          }).
          onError(error -> {
            System.out.println(":::" + error.getMessage());
          }).
          retry(3).
          closeCircuitBreaker(2000).
          onFailureRespond((t) -> Serializer.serialize(new Payload<>(t.getMessage()))).
          execute();
      System.out.println("-------2");
    }

    @Path("/simpleSyncNoConnectionAndExceptionErrorResponseStatefulFail")
    @GET
    public void simpleSyncNoConnectionAndExceptionErrorResponseStatefulFail(RestHandler reply) {
      System.out.println("-------1");
      reply.eventBusRequest().
          blocking().
          send("hello1", "welt").
          mapToByteResponse((handler) -> {
            System.out.println("value from event  ");
            return Serializer.serialize(new Payload<>(handler.result().body().toString()));
          }).
          onError(error -> {
            System.out.println(":::" + error.getMessage());
          }).
          retry(3).
          closeCircuitBreaker(2000).
          onFailureRespond((t) -> {
            throw new NullPointerException("nullpointer in onFailureRespond");
          }).
          execute();
      System.out.println("-------2");
    }


    @Path("/simpleSyncNoConnectionAndExceptionErrorResponseStateful")
    @GET
    public void simpleSyncNoConnectionAndExceptionErrorResponseStateful(RestHandler reply) {
      System.out.println("-------1");
      reply.eventBusRequest().
          blocking().
          send("hello1", "welt").
          mapToByteResponse((handler) -> {
            System.out.println("value from event  ");
            return Serializer.serialize(new Payload<>(handler.result().body().toString()));
          }).
          onError(error -> {
            System.out.println(":::" + error.getMessage());
          }).
          retry(3).
          closeCircuitBreaker(2000).
          onFailureRespond((t) -> {
            throw new NullPointerException("nullpointer in onFailureRespond");
          }).
          execute();
      System.out.println("-------2");
    }

    @OnRestError("/simpleSyncNoConnectionAndExceptionErrorResponseStateful")
    @GET
    public void simpleSyncNoConnectionAndExceptionErrorResponseStatefulOnFail(RestHandler reply,
        Throwable tt) {
      System.out.println("-------1");
      reply.response().
          blocking().
          byteResponse(() -> {
            System.out.println("value from event  ");
            return Serializer.serialize(new Payload<>("fallback response " + tt.getMessage()));
          }).
          onError(error -> {
            System.out.println(":::" + error.getMessage());
          }).
          retry(3).
          onFailureRespond((t) -> Serializer.serialize(new Payload<>(t.getMessage()))).
          execute();
      System.out.println("-------2");
    }


  }


  public class TestVerticle extends AbstractVerticle {

    public void start(io.vertx.core.Future<Void> startFuture) throws Exception {
      System.out.println("start");
      vertx.eventBus().consumer("hello", handler -> {
        System.out.println("request::" + handler.body().toString());
        handler.reply("hello");
      });
      startFuture.complete();
    }
  }

  public class TestErrorVerticle extends AbstractVerticle {

    private AtomicLong counter = new AtomicLong(0L);

    public void start(io.vertx.core.Future<Void> startFuture) throws Exception {
      System.out.println("start");
      vertx.eventBus().consumer("error", handler -> {
        System.out.println("request::" + handler.body().toString());
        if (counter.incrementAndGet() % 3 == 0) {
          System.out.println("reply::" + handler.body().toString());
          handler.reply("hello");
        } else {
          System.out.println("fail::" + handler.body().toString());
          handler.fail(500, "my error");
        }

      });
      startFuture.complete();
    }
  }


}