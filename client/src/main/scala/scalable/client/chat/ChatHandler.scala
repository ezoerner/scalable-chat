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

package scalable.client.chat

import java.util.UUID

import akka.actor.ActorLogging

/**
 * Actor to handle client-side chat events for one chat room.
 *
 * @author Eric Zoerner <a href="mailto:eric.zoerner@gmail.com">eric.zoerner@gmail.com</a>
 */

trait ChatHandler {
  this: ActorLogging ⇒

  def noListener(roomName: String) = log.error(s"Could not find chat room $roomName")

  var listeners = Map[String, ChatListener]()

  def handleJoined(username: String, roomName: String) =  {
    listeners.get(roomName).fold(noListener(roomName))(listener ⇒ listener.joined(username))
  }

  def handleChat(id: UUID, username: String, roomName: String, htmlText: String) = {
    listeners.get(roomName).fold(noListener(roomName))(listener ⇒ listener.receiveChat(id, username, htmlText))
  }

  def addChatListener(listener: ChatListener, room: String) = {
    //log.debug(s"Adding chat listener for $room")
    listeners = listeners + (room → listener)
  }

  def join(username: String, roomName: String): Unit
}

trait ChatListener {
  def joined(username: String): Unit
  def receiveChat(id: UUID, sender: String, htmlText: String) : Unit
}
