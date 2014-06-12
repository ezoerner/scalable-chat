package scalable.infrastructure.api

import akka.actor.{Actor, ActorSystem, Props}
import scalable.Global
import scalable.infrastructure.api.ResultStatus._
import org.specs2.mutable.Specification

/**
 * Tests BSON serialization and deserialization of messages
 *
 * @author Eric Zoerner <a href="mailto:eric.zoerner@gmail.com">eric.zoerner@gmail.com</a>
 */
class SerializableMessageTest extends Specification {

  class TestActor extends Actor {
    override def receive: Receive = {case msg ⇒ println(msg)}
  }

  "A Login message" should {

    "Convert to ByteString with type code and back without knowing type upfront" in {
      lazy val actorSystem = ActorSystem("Main")
      Global._defaultSystem = actorSystem
      val ref = actorSystem.actorOf(Props(new TestActor()))
      val login = AskLogin("user", "password", ref)
      val newLogin: SerializableMessage[_] = SerializableMessage(login.toByteString)
      val newRef = newLogin.asInstanceOf[AskLogin].replyTo
      actorSystem.shutdown()
      newLogin === login
    }
  }

  "A LoginResult message" should {
    "Convert to ByteString and back" in {
      lazy val actorSystem = ActorSystem("Main")
      Global._defaultSystem = actorSystem
      val ref = actorSystem.actorOf(Props(new TestActor()))
      val loginResult = LoginResult(Ok, "username", ref)
      val bytes = loginResult.toByteString
      val newLoginResult = SerializableMessage(bytes)
      actorSystem.shutdown()
      newLoginResult === loginResult
    }
  }

  "A Joined message" should {
    "convert to ByteString and back" in {
      val joined = Joined("username", "roomname")
      val bytes = joined.toByteString
      val newJoined = SerializableMessage(bytes)
      joined === newJoined
    }
  }


}
