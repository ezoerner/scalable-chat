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

import akka.actor._
import akka.persistence._
import com.datastax.driver.core.utils.UUIDs

import scala.collection.SortedSet
import scalable.messaging.api._

/** Chat room actor.
  *
  * @author Eric Zoerner <a href="mailto:eric.zoerner@gmail.com">eric.zoerner@gmail.com</a>
  */

trait Event
case class AddParticipant(username: String, connector: ActorRef) extends Event
case class RemoveParticipant(username: String) extends Event
case class AddChat(chat: Chat) extends Event

case class ChatRoomState(
    participants: Map[String, ActorRef] = Map.empty,
    messageHistory: SortedSet[Chat] = ChatRoom.newMessageHistory
) {
  def updated(event: Event): ChatRoomState = event match {
    case AddParticipant(username, connector) ⇒ copy(participants = participants + (username → connector))
    case RemoveParticipant(username)         ⇒ copy(participants = participants - username)
    case AddChat(chat)                       ⇒ copy(messageHistory = messageHistory + chat)
  }
}

object ChatRoom {
  def props(roomName: String) = Props(new ChatRoom(roomName))

  def newMessageHistory: SortedSet[Chat] = SortedSet[Chat]()(Ordering.by { _.id.get.timestamp })
}

class ChatRoom(private val roomName: String) extends PersistentActor with ActorLogging {

  override def persistenceId: String = s"chatroom-$roomName"

  var state = ChatRoomState()

  def updateState(event: Event): Unit = state = state.updated(event)

  def broadcast(msg: SerializableMessage): Unit =
    state.participants.values.foreach(_ ! msg)

  override val receiveCommand: Receive = {

    case (msg @ Join(username, rmName), connector: ActorRef) ⇒
      assert(rmName == roomName)
      val event = AddParticipant(username, connector)
      val oldState = state
      updateState(event)
      val stateChanged = state.participants.size > oldState.participants.size
      if (stateChanged) {
        broadcast(msg)
        // send history and participants messages to the joining user
        connector ! RoomInfo(roomName, state.messageHistory.toList, state.participants.keySet.toList.sorted)
      }

    case msg @ LeaveChat(username, room) ⇒
      assert(room == roomName, room)
      val event = RemoveParticipant(username)
      val oldState = state
      updateState(event)
      val stateChanged = state.participants.size < oldState.participants.size
      if (stateChanged)
        broadcast(msg)

    case msg @ Chat(id, _, rmName, _) ⇒
      assert(id.isEmpty)
      assert(rmName == roomName)
      val messageId = UUIDs.timeBased
      val chatWithId = msg.copy(id = Some(messageId))
      persist(AddChat(chatWithId)) { event ⇒
        updateState(event)
        broadcast(chatWithId)
      }

    case "snap"  ⇒ saveSnapshot(state)
    case "print" ⇒ println(state)
  }

  override val receiveRecover: Receive = {
    case evt: Event                                ⇒ updateState(evt)
    case SnapshotOffer(_, snapshot: ChatRoomState) ⇒ state = snapshot
  }
}

