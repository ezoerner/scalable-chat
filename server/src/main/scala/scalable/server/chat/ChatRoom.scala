package scalable.server.chat

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
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

  def broadcast(msg: SerializableMessage[_]): Unit =
    participants.values.foreach(_ ! msg)

  override def receive = {
    case ServerJoin(username, _roomName, connector) ⇒
      assert(_roomName == roomName)
      val newParticipants = participants + (username → connector)
      val notAlreadyPresent = newParticipants.size > participants.size
      participants = newParticipants
      if (notAlreadyPresent) {
        broadcast(Joined(username, roomName))
      }
    case ServerAskParticipants(_roomName, replyTo, connector) ⇒
      assert(_roomName == roomName)
      connector ! Participants(roomName, participants.keySet.toList.sorted, replyTo)
  }
}
