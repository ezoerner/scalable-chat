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

import akka.actor.{ActorSystem, Actor, Props}
import org.specs2.mutable.Specification

/**
 * Tests BSON serialization and deserialization of messages
 *
 * @author Eric Zoerner <a href="mailto:eric.zoerner@gmail.com">eric.zoerner@gmail.com</a>
 */
class SerializableMessageTest extends Specification {

  sequential

  class TestActor extends Actor {
    override def receive: Receive = {case msg ⇒ println(msg)}
  }

  "A Login message" should {

    "Convert to ByteString with type code and back without knowing type upfront" in {
      implicit lazy val actorSystem = ActorSystem("Main")
      try {
        val ref = actorSystem.actorOf(Props(new TestActor()))
        val login: SerializableMessage = AskLogin("user", "password", pickleActorRef(ref))
        val bytes = login.toByteString
        val newLogin: SerializableMessage = SerializableMessage(bytes)
        newLogin.asInstanceOf[AskLogin].username === login.asInstanceOf[AskLogin].username
        newLogin.asInstanceOf[AskLogin].password === login.asInstanceOf[AskLogin].password
        unpickleActorRef(newLogin.asInstanceOf[AskLogin].replyTo) ===
          unpickleActorRef(login.asInstanceOf[AskLogin].replyTo)
      } finally {
        actorSystem.shutdown()
      }
    }
  }

  "A LoginResult message" should {
    "Convert to ByteString and back" in {
      implicit lazy val actorSystem = ActorSystem("Main")
      try {
        val ref = actorSystem.actorOf(Props(new TestActor()))
        val loginResult: SerializableMessage = LoginResult(Ok(), "username", pickleActorRef(ref))
        val bytes = loginResult.toByteString
        val newLoginResult = SerializableMessage(bytes)
        newLoginResult.asInstanceOf[LoginResult].username === loginResult.asInstanceOf[LoginResult].username
        newLoginResult.asInstanceOf[LoginResult].result === loginResult.asInstanceOf[LoginResult].result
        unpickleActorRef(newLoginResult.asInstanceOf[LoginResult].replyTo) ===
          unpickleActorRef(loginResult.asInstanceOf[LoginResult].replyTo)
      } finally {
        actorSystem.shutdown()
      }
    }
  }

  "A Joined message" should {
    "convert to ByteString and back" in {
      implicit lazy val actorSystem = ActorSystem("Main")
      try {
        val joined: SerializableMessage = Joined("username", "roomName")
        val bytes = joined.toByteString
        val newJoined = SerializableMessage(bytes)
        joined === newJoined
      } finally {
        actorSystem.shutdown()
      }
    }
  }
}
