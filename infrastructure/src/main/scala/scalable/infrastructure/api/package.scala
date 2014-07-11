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

package scalable.infrastructure

import java.util.UUID

import akka.actor.{ ActorRef, ActorSystem }
import akka.serialization.SerializationExtension
import akka.util.ByteString

import scala.util.Try
import scala.pickling._
import scala.pickling.binary._

/**
 * Implicit BSON readers and writers for various data types.
 * Uses the global ActorSystem, so will not function if the global actor system
 * has not been initialized or has been shutdown.
 * @see GlobalEnv
 *
 * @author Eric Zoerner <a href="mailto:eric.zoerner@gmail.com">eric.zoerner@gmail.com</a>
 */
package object api {

  // TODO: Figure out how to create a custom pickler for ActorRefs

  def unpickleActorRef(bytes: ByteString)(implicit actorSystem: ActorSystem): ActorRef =
    specialUnpickle(bytes, classOf[ActorRef])

  def pickleActorRef(actorRef: ActorRef)(implicit actorSystem: ActorSystem): ByteString = {
    specialPickle(actorRef)
  }

  private def specialUnpickle[T](bytes: ByteString, clazz: Class[T])(implicit actorSystem: ActorSystem): T = {
    val tryT = SerializationExtension(actorSystem).deserialize[T](bytes.toArray, clazz)
    tryT.recover {
      case ex: Throwable ⇒
        ex.printStackTrace()
        throw ex
    }.get
  }

  private def specialPickle(serializable: AnyRef)(implicit actorSystem: ActorSystem): ByteString = {
    val tryBytes: Try[Array[Byte]] = SerializationExtension(actorSystem).serialize(serializable)
    val bytes: Array[Byte] = tryBytes.recover {
      case ex ⇒
        ex.printStackTrace()
        throw ex
    }.get
    ByteString(bytes)
  }
}
