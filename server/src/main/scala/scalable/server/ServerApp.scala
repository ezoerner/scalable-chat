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
import akka.cluster.ClusterEvent._
import akka.cluster.{ Cluster, MemberStatus }

import scala.concurrent.forkjoin.ThreadLocalRandom
import scalable.infrastructure.api._
import scalable.server.chat.ChatRoom
import scalable.server.tcp.{ ClientDisconnected, NewConnection, TcpService }

/**
 * Root actor of the server application.
 *
 * @author Eric Zoerner <a href="mailto:eric.zoerner@gmail.com">eric.zoerner@gmail.com</a>
 */
object ServerApp {
  val path = "/user/app"

  def props(servicePath: String) = Props(new ServerApp(servicePath))
}

class ServerApp(servicePath: String) extends Actor with ActorLogging {
  log.debug(s"Main Actor path=${self.path.toStringWithoutAddress}")

  val cluster = Cluster(context.system)
  val servicePathElements = servicePath match {
    case RelativeActorPath(elements) ⇒ elements
    case _                           ⇒ throw new IllegalArgumentException(s"servicePath [$servicePath] is not a valid relative actor path")
  }

  var nodes = Set.empty[Address]

  override def preStart(): Unit = {
    cluster.subscribe(self, classOf[MemberEvent], classOf[ReachabilityEvent])
  }

  override def postStop(): Unit = {
    cluster.unsubscribe(self)
  }

  context.actorOf(TcpService.props(self), "tcpService")
  lazy val lobbyChatRoom = context.actorOf(ChatRoom.props("Lobby"), "lobby")

  override def receive = {
    case msg: AskLogin ⇒
      log.debug(s"Received $msg")
      login(msg, sender())
    case msg: NewConnection ⇒
      // not interested
      log.debug(s"Received $msg")
    case msg: Join ⇒
      assert(msg.roomName == "Lobby") // "Lobby" is currently the only chat room
      lobbyChatRoom ! (msg, sender())
    case msg: LeaveChat ⇒
      assert(msg.roomName == "Lobby")
      lobbyChatRoom ! msg
    case msg: Chat ⇒
      assert(msg.roomName == "Lobby")
      lobbyChatRoom ! msg
    case msg @ ClientDisconnected(username) ⇒
      // for now, just let the lobby know that the user has disconnected,
      // TODO: have the user session track which chatrooms the user is in and
      // send the disconnected message to the user session, then the user
      // session can send leave messages to all the chat rooms the user is in
      lobbyChatRoom ! LeaveChat(username, "Lobby")
    case state: CurrentClusterState ⇒
      nodes = state.members.collect {
        case m if m.hasRole("service") && m.status == MemberStatus.Up ⇒ m.address
      }
    case MemberUp(m) if m.hasRole("service")        ⇒ nodes += m.address
    case other: MemberEvent                         ⇒ nodes -= other.member.address
    case UnreachableMember(m)                       ⇒ nodes -= m.address
    case ReachableMember(m) if m.hasRole("service") ⇒ nodes += m.address

    case msg                                        ⇒ log.error(s"Received unexpected message: $msg")
  }

  private def login(login: AskLogin, connector: ActorRef): Unit = {
    // We use dead simple authentication logic.
    // if a session already exists for this user then the passwords will be checked.
    // if the passwords are the same then the user rejoins the session
    // otherwise the username is already taken and login is rejected

    // pick a random service node in the cluster
    val address = nodes.toIndexedSeq(ThreadLocalRandom.current.nextInt(nodes.size))
    val userSessionService = context.actorSelection(RootActorPath(address) / servicePathElements)
    userSessionService ! (login, connector)
  }
}