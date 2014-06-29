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

package scalable.client

import java.net.InetSocketAddress

import akka.actor._
import akka.io.Tcp.Connected
import scalable.client.chat.ChatHandler
import scalable.client.tcp.TcpClient
import scalable.infrastructure.api.{Chat, Join, Joined}

import scala.reflect.runtime.universe.typeOf
import scalafx.application.JFXApp.PrimaryStage
import scalafx.application.Platform
import scalafx.scene.Scene
import scalafxml.core.DependenciesByType

/**
 * Root actor, used for tracking the user's client session information.
 *
 * @author Eric Zoerner <a href="mailto:eric.zoerner@gmail.com">eric.zoerner@gmail.com</a>
 */
object ClientApp {
  val path = "root"
  def props = Props(new ClientApp())
}

class ClientApp extends Actor with ActorLogging with ChatHandler {
  log.debug(s"ClientAppSupervisor path=${self.path.toStringWithoutAddress}")

  def tcpProps = TcpClient.props(new InetSocketAddress(Configuration.host, Configuration.portTcp), self)
  val tcpClient = context.actorOf(tcpProps, TcpClient.path)

  def openLobby(username: String): Unit = Platform.runLater {
    val root = loadFxmlFile("Lobby.fxml", new DependenciesByType(Map(typeOf[String] → username,
                                                                     typeOf[ActorSystem] → context.system,
                                                                     typeOf[ChatHandler] → this,
                                                                     typeOf[String] → username)))
    new PrimaryStage() {
      title = "Lobby"
      scene = new Scene(root)
    }.show()
    ()
  }

  override def join(username: String, roomName: String) =
    tcpClient ! Join(username, roomName)

  override def receive = {
    case msg: Connected ⇒ log.info(msg.toString)
    case OpenLobby(username) ⇒ openLobby(username)
    case Joined(username, roomName) ⇒ handleJoined(username, roomName)
    case Chat(id, username, roomName, htmlText) ⇒ handleChat(id.get, username, roomName, htmlText)
    case msg ⇒ log.info(s"Supervisor received: $msg")
  }
}

object Terminator {
  def props(actor: ActorRef) = Props(new Terminator(actor))
  def path = "terminator"
}

class Terminator(actor: ActorRef) extends Actor with ActorLogging {
  context watch actor
  def receive = {
    case Terminated(_) ⇒
      log.info("{} has terminated, shutting down system", actor.path)
      context.system.shutdown()
  }
}

case class OpenLobby(username: String)
