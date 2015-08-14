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

import scala.concurrent.duration._
import scala.util.Random
import scalable.infrastructure.api._
import scalable.infrastructure.tcp.SimpleBuffer

/** Tcp Client.
  *
  * @author Eric Zoerner <a href="mailto:eric.zoerner@gmail.com">eric.zoerner@gmail.com</a>
  */
object TcpClient {
  def props(systemListener: ActorRef) = Props(new TcpClient(systemListener))
  val path = "tcp"
}

class TcpClient(systemListener: ActorRef) extends Actor with ActorLogging {
  import akka.io.Tcp._
  import context.{ dispatcher, system }

  private val simpleBuffer = new SimpleBuffer
  private var connectMsg: Option[Connect] = None
  private var retryCount = 0
  private def resetRetryCount() = retryCount = 0

  private def exponentialBackOff(): FiniteDuration = {
      def nextDelay(r: Int) = (scala.math.pow(2, r - 1).round * 100 * (Random.nextDouble + 1)).milliseconds
    val delay = if (retryCount == 0) Duration.Zero else nextDelay(retryCount)
    retryCount = retryCount + 1
    log.info(s"Reconnect attempt #$retryCount after ${delay.toMillis / 1000.0f} seconds...")
    delay
  }

  private def handleDataReceived(data: ByteString): Unit = {
    log.debug(s"Attempting to deserialize data as SerializableMessage from ByteString length ${data.size}")
    simpleBuffer.nextMessageBytes(data).foreach { bytes ⇒
      val msg = SerializableMessage(bytes)
      log.debug("Received " + (msg match {
        case _: RoomInfo ⇒ "RoomInfo(...)"
        case m           ⇒ m.toString
      }))
      systemListener ! msg
    }
  }

  def receive = {
    case msg: Connect ⇒
      connectMsg = Some(msg)
      IO(Tcp) ! msg

    case CommandFailed(_: Connect) ⇒
      system.scheduler.scheduleOnce(exponentialBackOff(), IO(Tcp), connectMsg.get)

    case c @ Connected(remoteAddress, localAddress) ⇒
      resetRetryCount()
      systemListener ! c
      val connection = sender()
      connection ! Register(self)
      context become {
        case msg: SerializableMessage ⇒
          log.debug(s"Writing $msg")
          connection ! Write(simpleBuffer.serializableMessageWithLength(msg))
        case Received(data) ⇒
          log.debug(s"Received data of class: ${data.getClass.getName}")
          handleDataReceived(data)
        case CommandFailed(w: Write) ⇒
          // O/S buffer was full
          systemListener ! "write failed"
        case msg: ConnectionClosed if !msg.isPeerClosed ⇒
          // close was initiated by this side (the client), so don't try to reconnect
          context.unbecome()
          systemListener ! msg
        case msg: ConnectionClosed if msg.isPeerClosed ⇒
          log.error("Connection closed, attempting to reconnect..")
          context.unbecome()
          system.scheduler.scheduleOnce(exponentialBackOff(), IO(Tcp), connectMsg.get)
        case "close" ⇒
          connection ! Close
        case msg ⇒
          log.error(s"Unexpected message received: $msg")
      }
  }
}