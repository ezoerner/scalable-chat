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
  def props(systemListener: ActorRef) = Props(new TcpClient(systemListener))
  val path = "tcp"
}

class TcpClient(systemListener: ActorRef) extends Actor with ActorLogging {
  import akka.io.Tcp._
  import context.system

  private def handleDataReceived(data: ByteString): Unit = {
    log.debug("Attempting to deserialize data as SerializableMessage")
    val msg = SerializableMessage(data)
    log.debug(s"received $msg")
    systemListener ! msg
  }

  def receive = {
    case msg: Connect ⇒
      IO(Tcp) ! msg

    case CommandFailed(_: Connect) ⇒
      systemListener ! "connect failed"

    case c @ Connected(remoteAddress, localAddress) ⇒
      systemListener ! c
      val connection = sender()
      connection ! Register(self)
      context become {
        case msg: SerializableMessage ⇒
          log.debug(s"Writing $msg")
          connection ! Write(msg.toByteString)
        case Received(data) ⇒
          log.debug(s"Received data of class: ${data.getClass.getName}")
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