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

import akka.actor.{Actor, ActorLogging}
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scalable.client._
import scalable.client.tcp.ClientAskParticipants
import scalable.infrastructure.api.{Chat, Join, LeaveChat, Participants}




/**
 * Actor to handle client-side chat events for all chat rooms.
 *
 * @author Eric Zoerner <a href="mailto:eric.zoerner@gmail.com">eric.zoerner@gmail.com</a>
 */

trait ChatHandler {
  this: Actor with ActorLogging ⇒

  private val tcpClient = tcpClientSelection(context.system)
  private implicit val timeout: Timeout = 5.second // TODO: make this configurable

  def noListener(roomName: String) = log.error(s"Could not find chat room $roomName")

  var listeners = Map[String, ChatListener]()

  def getInitialParticipants(roomName: String): Future[List[String]] = {
    val futureResponse = (tcpClient ? ClientAskParticipants(roomName)).mapTo[Participants]
    futureResponse.map(_.participants)
  }

  def handleJoined(username: String, roomName: String) =
    listeners.get(roomName).fold(noListener(roomName))(listener ⇒ listener.joined(username))

  def handleLeft(username: String, roomName: String) =
    listeners.get(roomName).fold(noListener(roomName))(listener ⇒ listener.left(username))

  def handleChat(id: UUID, username: String, roomName: String, htmlText: String) = {
    listeners.get(roomName).fold(noListener(roomName))(listener ⇒ listener.receiveChat(id, username, htmlText))
  }

  def addChatListener(listener: ChatListener, room: String) = {
    listeners = listeners + (room → listener)
  }

  def join(username: String, roomName: String): Unit =
    tcpClient ! Join(username, roomName)

  def leave(roomName: String, username: String): Unit =
    tcpClient ! LeaveChat(username, roomName)

  def sendChat(roomName: String, username: String, htmlText: String) =
    tcpClient ! Chat(None, username, roomName, htmlText)
}

trait ChatListener {
  def joined(username: String): Unit
  def left(username: String): Unit
  def receiveChat(id: UUID, sender: String, htmlText: String) : Unit
}
