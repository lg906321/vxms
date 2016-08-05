package or.jacpfx.spi;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;

/**
 * Created by amo on 05.08.16.
 */
public interface RESThandlerSPI {

    void initRESTHandler(Vertx vertx, Router router, JsonObject config, AbstractVerticle service);
}
