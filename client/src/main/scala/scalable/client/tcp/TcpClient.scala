package scalable.client.tcp

import java.net.InetSocketAddress

import akka.actor._
import akka.io.{IO, Tcp}
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
    val message = SerializableMessage(data)
    def warn(msg: SerializableMessage[_]) =
      log.warning(s"Received $msg with no listener")

    message match {
      case msg: LoginResult ⇒
        log.debug(s"received $msg")
        log.debug(s"Sending LoginResult to ${msg.replyTo}")
        msg.replyTo ! msg
      case msg: Joined ⇒
        log.debug(s"received $msg")
        systemListener ! msg
      case msg: Participants ⇒
        log.debug(s"received $msg")
        msg.replyTo ! msg
    }
  }

  def receive = {
    case CommandFailed(_: Connect) =>
      systemListener ! "connect failed"
      context stop self

    case c @ Connected(remoteAddress, localAddress) =>
      systemListener ! c
      val connection = sender()
      connection ! Register(self)
      context become {
        case msg: AskLogin1 ⇒
          log.debug(s"Received $msg from ${sender()}")
          connection ! Write(AskLogin(msg.username, msg.password, sender()).toByteString)
        case msg: Join ⇒
          log.debug(s"Writing $msg")
          connection ! Write(msg.toByteString)
        case msg: AskParticipants1 ⇒
          log.debug(s"Writing $msg")
          connection ! Write(AskParticipants(msg.roomName, sender()).toByteString)
        case Received(data) =>
          handleDataReceived(data)
        case CommandFailed(w: Write) =>
          // O/S buffer was full
          systemListener ! "write failed"
        case _: ConnectionClosed =>
          systemListener ! "connection closed"
          context stop self
        case "close" =>
          connection ! Close
        case msg ⇒
          log.error(s"Unexpected message received: $msg")
      }
  }
}

// short form of AskLogin to be filled in with replyTo
case class AskLogin1(username: String, password: String)
case class AskParticipants1(roomName: String)