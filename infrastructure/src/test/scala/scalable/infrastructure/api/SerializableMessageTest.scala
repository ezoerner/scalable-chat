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

package scalable.infrastructure.api

import java.util.UUID

import akka.actor.{ Actor, ActorSystem, Props }
import org.specs2.mutable.Specification

import scalable.infrastructure.api.ResultStatus._

/**
 * Tests serialization and deserialization of messages
 *
 * @author Eric Zoerner <a href="mailto:eric.zoerner@gmail.com">eric.zoerner@gmail.com</a>
 */
class SerializableMessageTest extends Specification {
  sequential

  class TestActor extends Actor {
    override def receive: Receive = { case msg ⇒ println(msg) }
  }

  def testMessages(actorSystem: ActorSystem): List[SerializableMessage] = {
    val ref = actorSystem.actorOf(Props(new TestActor()))
    List(History("room", List(Chat(Some(UUID.randomUUID()), "sender", "room", "html"),
      Chat(Some(UUID.randomUUID()), "sender", "room", "html"))),
      LoginResult(Ok, "user", ref),
      AskLogin("user", "password", ref),
      AskParticipants("room", ref),
      Join("user", "room"),
      LeaveChat("user", "room"),
      Chat(None, "sender", "room", "html"),
      Chat(Some(UUID.randomUUID()), "sender", "room", "html"),
      Participants("room", List("p1", "p2"), ref),
      Joined("user", "room"))
  }

  "Serializable messages" should {

    "Convert to ByteString with type code and back without knowing type upfront" in {
      implicit lazy val actorSystem = ActorSystem("Main")
      try {
        true === testMessages(actorSystem).forall { msg ⇒
          val bytes = msg.toByteString
          val newMsg = SerializableMessage(bytes)
          newMsg === msg
          true
        }
      }
      finally {
        actorSystem.shutdown()
      }
    }
  }
}
