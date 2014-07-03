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

package scalable.server.chat

import java.util.UUID

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.datastax.driver.core.utils.UUIDs

import scala.collection.SortedMap
import scalable.infrastructure.api._
import scalable.server.{ServerAskParticipants, ServerJoin}

/**
 * Chat room actor.
 *
 * @author Eric Zoerner <a href="mailto:eric.zoerner@gmail.com">eric.zoerner@gmail.com</a>
 */
object ChatRoom {
  def props(roomName: String) = Props(new ChatRoom(roomName))
}

class ChatRoom(private val roomName: String) extends Actor with ActorLogging {
  var participants = Map[String, ActorRef]()
  val messageHistory = SortedMap[UUID,ChatHistory]()

  def broadcast(msg: SerializableMessage): Unit =
    participants.values.foreach(_ ! msg)

  override def receive = {
    case ServerJoin(username, rmName, connector) ⇒
      assert(rmName == roomName)
      val newParticipants = participants + (username → connector)
      val notAlreadyPresent = newParticipants.size > participants.size
      participants = newParticipants
      if (notAlreadyPresent)
        broadcast(Joined(username, roomName))
    case ServerAskParticipants(rmName, replyTo, connector) ⇒
      assert(rmName == roomName)
      connector ! Participants(roomName, participants.keySet.toList.sorted, replyTo)
    case msg @ LeaveChat(username, room) ⇒
      assert(room == roomName, room)
      val newParticipants = participants - username
      val notAlreadyAbsent = newParticipants.size < participants.size
      participants = newParticipants
      if (notAlreadyAbsent)
        broadcast(msg)
    case msg @ Chat(id, sender, rmName, htmlText) ⇒
      assert(id.isEmpty)
      assert(rmName == roomName)
      val messageId = UUIDs.timeBased
      messageHistory + (messageId → ChatHistory(sender, htmlText))
      broadcast(msg.copy(id = Some(messageId)))
  }

  case class ChatHistory(sender: String, htmlText: String)
}

