package scalable.client.chat

import akka.actor.ActorLogging

/**
 * Actor to handle client-side chat events for one chat room.
 *
 * @author Eric Zoerner <a href="mailto:eric.zoerner@gmail.com">eric.zoerner@gmail.com</a>
 */

trait ChatHandler {
  this: ActorLogging ⇒

  def warn() = log.warning(s"Received joined with no listener")

  var listeners = Map[String, ChatListener]()

  def handleJoined(username: String, roomName: String) =  {
    listeners.get(roomName).fold(warn())(listener ⇒ listener.joined(username, roomName))
  }

  def addChatListener(listener: ChatListener, room: String) = {
    //log.debug(s"Adding chat listener for $room")
    listeners = listeners + (room → listener)
  }

  def join(username: String, roomName: String): Unit
}

trait ChatListener {
  def joined(username: String, roomName: String): Unit
}
