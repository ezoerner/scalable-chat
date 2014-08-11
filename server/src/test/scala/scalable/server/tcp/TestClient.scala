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

import java.net.InetSocketAddress

import akka.actor.{ Actor, ActorRef, Props }
import akka.io.{ IO, Tcp }

import scalable.infrastructure.api.SerializableMessage
import scalable.infrastructure.tcp.SimpleBuffer

/**
 * Test tcp client.
 *
 * @author Eric Zoerner <a href="mailto:eric.zoerner@gmail.com">eric.zoerner@gmail.com</a>
 */
object TestClient {
  def props(remote: InetSocketAddress, replies: ActorRef) =
    Props(classOf[TestClient], remote, replies)
}

class TestClient(remote: InetSocketAddress, listener: ActorRef) extends Actor {

  import akka.io.Tcp._
  import context.system

  private val simpleBuffer = new SimpleBuffer

  IO(Tcp) ! Connect(remote)

  def receive = {
    case msg: Connect ⇒
      IO(Tcp) ! msg
      sender() ! msg
    case CommandFailed(_: Connect) ⇒
      listener ! "connect failed"
      context stop self

    case c @ Connected(remoteAddress, local) ⇒
      listener ! c
      val connection = sender()
      connection ! Register(self)
      context become {
        case msg: SerializableMessage ⇒
          connection ! Write(simpleBuffer.serializableMessageWithLength(msg))
        case CommandFailed(w: Write) ⇒
          // O/S buffer was full
          listener ! "write failed"
        case Received(data) ⇒
          simpleBuffer.nextMessageBytes(data).foreach { bytes ⇒
            val message = SerializableMessage(bytes)
            listener ! message
          }
        case "close" ⇒
          connection ! Close
        case _: ConnectionClosed ⇒
          listener ! "connection closed"
          context stop self
      }
  }
}