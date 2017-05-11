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

package org.jacpfx;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import org.jacpfx.common.ServiceEndpoint;
import org.jacpfx.vertx.rest.annotation.OnRestError;
import org.jacpfx.vertx.rest.response.RestHandler;
import org.jacpfx.vertx.services.VxmsEndpoint;

/**
 * Created by Andy Moncsek on 25.01.16.
 */
@ServiceEndpoint(port = 9090)
public class SimpleREST extends VxmsEndpoint {

  @Path("/helloGET")
  @GET
  public void simpleRESTHello(RestHandler handler) {
    handler.
        response().
        stringResponse((response) -> response.complete("simple response")).
        execute();
  }


  @Path("/helloGET/:name")
  @GET
  public void simpleRESTHelloWithParameter(RestHandler handler) {

    handler.
        eventBusRequest().
        send("target.id", "message").
        mapToStringResponse((result, future) -> {
          Message<Object> message = result.result();
          future.complete("hello " + message.body().toString());
        }).execute();
    handler.
        response().
        stringResponse((response) ->
            response.complete("hello World " + handler.request().param("name"))).
        timeout(2000).
        onFailureRespond((error, future) -> future.complete("error")).
        httpErrorCode(HttpResponseStatus.BAD_REQUEST).
        retry(3).
        closeCircuitBreaker(2000).
        execute();
  }

  @OnRestError("/helloGET/:name")
  public void simpleRESTHelloWithParameterError(RestHandler handler, Throwable t) {

  }

  public static void main(String[] args) {
    DeploymentOptions options = new DeploymentOptions().setInstances(1)
        .setConfig(new JsonObject().put("host", "localhost"));
    Vertx.vertx().deployVerticle(SimpleREST.class.getName(), options);
  }
}
