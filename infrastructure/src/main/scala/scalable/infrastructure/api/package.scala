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

import akka.actor.ActorRef
import akka.event.Logging
import akka.serialization.SerializationExtension
import reactivemongo.bson.buffer.ArrayBSONBuffer
import scalable.GlobalEnv._
import reactivemongo.bson._

/**
 * Implicit BSON readers and writers for various data types.
 * Uses the global ActorSystem, so will not function if the global actor system
 * has not been initialized or has been shutdown.
 * @see GlobalEnv
 *
 * @author Eric Zoerner <a href="mailto:eric.zoerner@gmail.com">eric.zoerner@gmail.com</a>
 */
package object api {

  trait SerializableReader[T <: Serializable] extends BSONReader[BSONBinary, T] {
    val clazz: Class[T]
    override def read(bson: BSONBinary): T = {
      val bytes = bson.value.readArray(bson.value.readable())
      val tryT = SerializationExtension(system).deserialize[T](bytes, clazz)
      tryT.recover {
        case ex: Throwable ⇒
          ex.printStackTrace()
          throw ex
      }.get
    }
  }

  trait SerializableWriter[T <: AnyRef] extends BSONWriter[T, BSONValue] {

    override def write(serializable: T): BSONValue = {
      val tryBytes = SerializationExtension(system).serialize(serializable)
      val bytes = tryBytes.recover {
        case ex: Throwable ⇒
          ex.printStackTrace()
          throw ex
      }.get
      BSONBinary(bytes, Subtype.GenericBinarySubtype)
    }
  }

  implicit object ActorRefReader extends SerializableReader[ActorRef] {
    val clazz: Class[ActorRef] = classOf[ActorRef]
  }

  implicit object ActorRefWriter extends SerializableWriter[ActorRef]

  implicit object UuidReader extends BSONReader[BSONBinary, UUID] {
    override def read(bson: BSONBinary): UUID = {
      val mostSigBits = bson.value.readLong()
      val leastSigBits = bson.value.readLong()
      assert(bson.value.readable() == 0)
      new UUID(mostSigBits, leastSigBits)
    }
  }

  implicit object UuidWriter extends BSONWriter[UUID, BSONBinary] {
    override def write(uuid: UUID): BSONBinary = {
      val writableBuffer = new ArrayBSONBuffer()
      writableBuffer.writeLong(uuid.getMostSignificantBits).writeLong(uuid.getLeastSignificantBits)
      BSONBinary(writableBuffer.toReadableBuffer(), Subtype.GenericBinarySubtype)
    }
  }

}
