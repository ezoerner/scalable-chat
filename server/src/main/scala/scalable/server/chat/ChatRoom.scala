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

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.datastax.driver.core.utils.UUIDs

import scala.collection.{SortedSet, mutable}
import scalable.infrastructure.api._

/**
 * Chat room actor.
 *
 * @author Eric Zoerner <a href="mailto:eric.zoerner@gmail.com">eric.zoerner@gmail.com</a>
 */
object ChatRoom {
  def props(roomName: String) = Props(new ChatRoom(roomName))

  // for now history is only chat messages, may need to expand that later;
  // ordering by timestamp alone is good enough, if there are multiple messages with the same
  // timestamp then the ordering between them is non-deterministic
  def newMessageHistory: SortedSet[Chat] = SortedSet[Chat]()(Ordering.by { _.id.get.timestamp })
}

case class ChatRoomState(participants: Map[String, ActorRef] = Map.empty,
                         messageHistory: SortedSet[Chat] = ChatRoom.newMessageHistory) {

}

class ChatRoom(private val roomName: String) extends /*Persistent*/ Actor with ActorLogging {
  var participants = Map[String, ActorRef]()
  val messageHistory = mutable.SortedSet[Chat]()(Ordering.by { _.id.get.timestamp })

  //override def persistenceId: String = s"chatroom-$roomName"

  //var state = ChatRoomState()

  def broadcast(msg: SerializableMessage): Unit =
    participants.values.foreach(_ ! msg)

  override def receive /*Command*/ : Receive = {

    case (Join(username, rmName), connector: ActorRef) ⇒
      assert(rmName == roomName)
      val newParticipants = participants + (username → connector)
      val notAlreadyPresent = newParticipants.size > participants.size
      participants = newParticipants
      if (notAlreadyPresent) {
        broadcast(Join(username, roomName))
        // send history and participants messages to the joining user
        connector ! RoomInfo(roomName, messageHistory.toList, participants.keySet.toList.sorted)
      }

    case msg @ LeaveChat(username, room) ⇒
      assert(room == roomName, room)
      val newParticipants = participants - username
      val notAlreadyAbsent = newParticipants.size < participants.size
      participants = newParticipants
      if (notAlreadyAbsent) {
        broadcast(msg)
      }

    case msg @ Chat(id, sender, rmName, htmlText) ⇒
      assert(id.isEmpty)
      assert(rmName == roomName)
      val messageId = UUIDs.timeBased
      val chatWithId = msg.copy(id = Some(messageId))
      messageHistory += chatWithId
      broadcast(chatWithId)
  }

  //override def receiveRecover: Receive = ???

}

