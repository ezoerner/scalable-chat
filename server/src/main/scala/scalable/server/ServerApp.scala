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

package scalable.server

import akka.actor._
import scalable.infrastructure.api._
import scalable.server.chat.ChatRoom
import scalable.server.tcp.{ ClientDisconnected, NewConnection, TcpService }

import scala.util.Try

/**
 * Root actor of the server application.
 *
 * @author Eric Zoerner <a href="mailto:eric.zoerner@gmail.com">eric.zoerner@gmail.com</a>
 */
object ServerApp {
  val path = "/user/app"
}

class ServerApp extends Actor with ActorLogging {
  log.debug(s"Main Actor path=${self.path.toStringWithoutAddress}")
  context.actorOf(TcpService.props(self), "tcpService")
  lazy val lobbyChatRoom = context.actorOf(ChatRoom.props("Lobby"), "lobby")

  private def login(login: ServerLogin): Unit = {
    // We use dead simple authentication logic.
    // if a session already exists for this user then the passwords will be checked.
    // if the passwords are the same then the user rejoins the session
    // otherwise the username is already taken and login is rejected

    def createSession(login: ServerLogin): Try[ActorRef] = {
      Try(context.actorOf(UserSession.props(login), login.username))
    }

    val newSession = createSession(login)
    newSession.recover {
      case _: InvalidActorNameException ⇒
        // existing session will verify password and send back a LoginResult
        context.actorSelection(login.username) ! login
      case ex ⇒ throw ex
    }
  }

  override def receive = {
    case msg: AskLogin ⇒
      log.debug(s"Received $msg")
      login(ServerLogin(msg.username, msg.password, msg.replyTo, sender()))
    case msg: NewConnection ⇒
      // not interested
      log.debug(s"Received $msg")
    case msg: Join ⇒
      assert(msg.roomName == "Lobby") // "Lobby" is currently the only top-level chat room
      lobbyChatRoom ! ServerJoin(msg.username, msg.roomName, sender())
    case msg: LeaveChat ⇒
      assert(msg.roomName == "Lobby")
      lobbyChatRoom ! msg
    case msg: AskParticipants ⇒
      assert(msg.roomName == "Lobby")
      lobbyChatRoom ! ServerAskParticipants(msg.roomName, msg.replyTo, sender())
    case msg: Chat ⇒
      assert(msg.roomName == "Lobby")
      lobbyChatRoom ! msg
    case msg @ ClientDisconnected(username) ⇒
      // for now, just let the lobby know that the user has disconnected,
      // TODO: have the user session track which chatrooms the user is in and
      // send the disconnected message to the user session, then the user
      // session can send leave messages to all the chat rooms the user is in
      lobbyChatRoom ! LeaveChat(username, "Lobby") // HACK
    case msg ⇒ log.error(s"Received unexpected message: $msg")
  }
}

case class ServerLogin(username: String, password: String, replyTo: ActorRef, connector: ActorRef)
case class ServerJoin(username: String, roomName: String, connector: ActorRef)
case class ServerAskParticipants(roomName: String, replyTo: ActorRef, connector: ActorRef)
