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

package scalable.messaging.api

import java.util.UUID

import akka.actor.{Actor, ActorSystem}
import akka.util.ByteString
import org.specs2.mutable.Specification

import scalable.messaging.api.ResultStatus._

/** Tests serialization and deserialization of messages
  *
  * @author Eric Zoerner <a href="mailto:eric.zoerner@gmail.com">eric.zoerner@gmail.com</a>
  */
class SerializableMessageTest extends Specification {
  sequential

  class TestActor extends Actor {
    override def receive: Receive = { case msg ⇒ println(msg) }
  }

  def testMessages(actorSystem: ActorSystem): List[SerializableMessage] = {
    List(
      RoomInfo("room", List(
        Chat(Some(UUID.randomUUID()), "sender", "room", "html"),
        Chat(Some(UUID.randomUUID()), "sender", "room", "html")
      ),
        List("part1", "part2")),
      LoginResult(Ok, "user"),
      AskLogin("user", "password"),
      Join("user", "room"),
      LeaveChat("user", "room"),
      Chat(None, "sender", "room", "html"),
      Chat(Some(UUID.randomUUID()), "sender", "room", "html")
    )
  }

  "Serializable messages" should {

    "Convert to ByteString with type code and back without knowing type upfront" in {
      implicit lazy val actorSystem = ActorSystem("Main")
      try {
        true === testMessages(actorSystem).forall { msg ⇒
          val bytes = ByteString(msg.toByteArray)
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
