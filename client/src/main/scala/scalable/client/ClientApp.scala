package scalable.client

import java.net.InetSocketAddress

import akka.actor._
import akka.io.Tcp.Connected
import scalable.client.chat.ChatHandler
import scalable.client.tcp.TcpClient
import scalable.infrastructure.api.{Join, Joined}

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

  override def join(username: String, roomName: String) = tcpClient ! Join(username, roomName)

  override def receive = {
    case msg: Connected ⇒ log.debug("Client Connected")
    case OpenLobby(username) ⇒ openLobby(username)
    case Joined(username, roomName) ⇒ handleJoined(username, roomName)
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
