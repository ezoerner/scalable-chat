package scalable.infrastructure

import akka.actor.ActorRef
import akka.event.Logging
import akka.serialization.SerializationExtension
import scalable.GlobalEnv._
import reactivemongo.bson._

/**
 * Implicit BSON readers and writers.
 *
 * @author Eric Zoerner <a href="mailto:eric.zoerner@gmail.com">eric.zoerner@gmail.com</a>
 */
package object api {
  private lazy val log = Logging(system, this.getClass)


  trait SerializableReader[T <: Serializable] extends BSONReader[BSONBinary, T] {
    val clazz: Class[T]
    override def read(bson: BSONBinary): T = {
      val bytes = bson.value.readArray(bson.value.readable())
      val tryT = SerializationExtension(system).deserialize[T](bytes, clazz)
      tryT.recover{
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

}
