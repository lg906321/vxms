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

package or.jacpfx.spi;

import io.vertx.core.AbstractVerticle;
import java.util.function.Consumer;

/**
 * Created by Andy Moncsek on 23.06.16.
 */
public interface ServiceDiscoverySpi {

  void registerService(Runnable onSuccess, Consumer<Throwable> onFail,
      AbstractVerticle verticleInstance);

  void disconnect();
}
