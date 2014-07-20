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
import javafx.scene.Parent
import javafx.{ scene ⇒ jfxs }

import akka.actor._
import akka.io.Tcp.{ Connect, Connected, ConnectionClosed }

import scala.reflect.runtime.universe.typeOf
import scalable.client.chat.{ ChatController, ChatHandler }
import scalable.client.login.{ LoginListener, LoginHandler }
import scalable.client.tcp.TcpClient
import scalable.infrastructure.api._
import scalafx.Includes._
import scalafx.application.JFXApp.PrimaryStage
import scalafx.application.Platform
import scalafx.scene.Scene
import scalafxml.core.{ DependenciesByType, FXMLLoader }

/**
 * Root actor, used for tracking the user's client session information.
 *
 * @author Eric Zoerner <a href="mailto:eric.zoerner@gmail.com">eric.zoerner@gmail.com</a>
 */
object ClientApp {
  val path = "root"
  def props(loginListener: LoginListener) = Props(new ClientApp(loginListener))
}

class ClientApp(loginListener: LoginListener)
    extends Actor with ActorLogging with ChatHandler with LoginHandler {
  private val tcpClient = context.actorOf(TcpClient.props(self), TcpClient.path)
  private var login: Option[AskLogin] = None

  addListener(loginListener)

  def connect(host: String, port: Int, loginMsg: AskLogin) = {
    login = Some(loginMsg)
    tcpClient ! Connect(new InetSocketAddress(host, port))
  }

  def openLobby(username: String): Unit = Platform.runLater {
    removeListener(loginListener)

    val loader: FXMLLoader = new FXMLLoader(getClass.getResource("Lobby.fxml"),
      new DependenciesByType(Map(typeOf[String] → username,
        typeOf[ActorSystem] → context.system,
        typeOf[ChatHandler] → this,
        typeOf[LoginHandler] → this,
        typeOf[String] → username)))
    loader.load()
    val root: Parent = loader.getRoot[jfxs.Parent]
    val controller = loader.getController[ChatController]()

    val stage: PrimaryStage = new PrimaryStage() {
      title = "Lobby"
      scene = new Scene(root)
    }
    stage.show()
    controller.setStageAndSetupListeners(stage)
  }

  override def receive = {
    case msg: Connected ⇒
      log.info(msg.toString)
      tcpClient ! login.get
    case (host: String, port: Int, msg: AskLogin)  ⇒ connect(host, port, msg)
    case OpenLobby(username)                       ⇒ openLobby(username)
    case Join(username, roomName)                  ⇒ handleJoined(username, roomName)
    case LeaveChat(username, roomName)             ⇒ handleLeft(username, roomName)
    case Chat(id, username, roomName, htmlText)    ⇒ handleChat(id.get, username, roomName, htmlText)
    case RoomInfo(roomName, history, participants) ⇒ handleRoomInfo(roomName, history, participants)
    case LoginResult(resultStatus, username)       ⇒ handleLoginResult(resultStatus, username)
    case msg: ConnectionClosed ⇒
      handleConnectionClosed()
    case msg ⇒ log.info(s"ClientApp received: $msg")
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
