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

package scalable.client.tcp

import java.net.InetSocketAddress

import akka.actor._
import akka.io.{ IO, Tcp }
import akka.util.ByteString
import scalable.infrastructure.api._

/**
 * Tcp Client.
 *
 * @author Eric Zoerner <a href="mailto:eric.zoerner@gmail.com">eric.zoerner@gmail.com</a>
 */
object TcpClient {
  def props(remote: InetSocketAddress, systemListener: ActorRef) = Props(new TcpClient(remote, systemListener))
  val path = "tcp"
}

class TcpClient(remote: InetSocketAddress, systemListener: ActorRef) extends Actor with ActorLogging {
  import akka.io.Tcp._
  import context.system

  IO(Tcp) ! Connect(remote)

  private def handleDataReceived(data: ByteString): Unit = {
    SerializableMessage(data) match {
      case msg: LoginResult ⇒
        log.debug(s"received $msg")
        log.debug(s"Sending LoginResult to ${msg.replyTo}")
        unpickleActorRef(msg.replyTo) ! msg
      case msg: Participants ⇒
        log.debug(s"received $msg")
        unpickleActorRef(msg.replyTo) ! msg
      case msg: SerializableMessage ⇒
        log.debug(s"received $msg")
        systemListener ! msg
    }
  }

  def receive = {
    case CommandFailed(_: Connect) ⇒
      systemListener ! "connect failed"
      context stop self

    case c @ Connected(remoteAddress, localAddress) ⇒
      systemListener ! c
      val connection = sender()
      connection ! Register(self)
      context become {
        case msg: CorrelatedRequest ⇒
          log.debug(s"Received $msg from ${sender()}")
          connection ! Write(msg.remoteRequest(sender()).toByteString)
        case msg: SerializableMessage ⇒
          log.debug(s"Writing $msg")
          connection ! Write(msg.toByteString)
        case Received(data) ⇒
          handleDataReceived(data)
        case CommandFailed(w: Write) ⇒
          // O/S buffer was full
          systemListener ! "write failed"
        case _: ConnectionClosed ⇒
          systemListener ! "connection closed"
          context stop self
        case "close" ⇒
          connection ! Close
        case msg ⇒
          log.error(s"Unexpected message received: $msg")
      }
  }
}

trait CorrelatedRequest {
  def remoteRequest(replyTo: ActorRef)(implicit system: ActorSystem): SerializableMessage
}

case class ClientAskLogin(username: String, password: String) extends CorrelatedRequest {
  override def remoteRequest(replyTo: ActorRef)(implicit system: ActorSystem) = {
    AskLogin(username, password, pickleActorRef(replyTo))
  }
}

case class ClientAskParticipants(roomName: String) extends CorrelatedRequest {
  override def remoteRequest(replyTo: ActorRef)(implicit system: ActorSystem) = {
    AskParticipants(roomName, pickleActorRef(replyTo))
  }
}
