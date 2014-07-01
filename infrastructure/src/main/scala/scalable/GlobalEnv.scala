/*
 * Copyright 2014 Eric Zoerner
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

package scalable

import akka.actor.ActorSystem
import java.util.concurrent.atomic.AtomicReference

/**
 * Global settings for this environment.
 * Maintains an implicit global ActorSystem. If the application relies on this
 * it will only function in the context of a running system.
 *
 * @author Eric Zoerner <a href="mailto:eric.zoerner@gmail.com">eric.zoerner@gmail.com</a>
 */

object GlobalEnv {

  private val _systemRef = new AtomicReference[ActorSystem]()

  private def unsafeSystem: ActorSystem = _systemRef.get
  private def maybeSystem: Option[ActorSystem] = Option(unsafeSystem)

  implicit def system: ActorSystem = maybeSystem.getOrElse(sys.error("There is no started ActorSystem"))


  def createActorSystem(systemName: String): ActorSystem = {
    val sys = ActorSystem(systemName)
    if (!_systemRef.compareAndSet(null, sys))
      throw new IllegalStateException("Global ActorSystem has already been created")
    sys
  }

  def shutdownActorSystem() = {
    val sys = system
    _systemRef.set(null)
    sys.shutdown()
  }
}
