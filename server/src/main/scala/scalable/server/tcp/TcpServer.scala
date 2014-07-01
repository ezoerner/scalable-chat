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

package scalable.server.tcp

import akka.actor._
import scalable.infrastructure.api._

object TcpServer {
  def props(connection: ActorRef, listener: ActorRef) = Props(new TcpServer(connection, listener))
}

class TcpServer(private val connection : ActorRef, private val listener: ActorRef)
extends Actor with ActorLogging {
  implicit val system = context.system

  import akka.io.Tcp._

  listener ! NewConnection(self)

  override def receive = {
    case Received(byteString) =>
      val message = SerializableMessage(byteString)
      log.debug(s"Received $message")
      message match {
        case msg: AskLogin ⇒ listener ! msg
        case msg: Join ⇒ listener ! msg
        case msg: AskParticipants ⇒ listener ! msg
      }
    case PeerClosed => stop()
    case ErrorClosed => stop()
    case Closed => stop()
    case ConfirmedClosed => stop()
    case Aborted => stop()

    case msg: LoginResult ⇒
      log.debug(s"Writing $msg to connection")
      connection ! Write(msg.toByteString)
    case msg: Joined ⇒
      log.debug(s"Writing $msg to connection")
      connection ! Write(msg.toByteString)
    case msg: Participants ⇒
      log.debug(s"Writing $msg to connection")
      connection ! Write(msg.toByteString)
  }

  private def stop(): Unit = {
    context stop self
  }
}

case class NewConnection(serverConnector: ActorRef)
