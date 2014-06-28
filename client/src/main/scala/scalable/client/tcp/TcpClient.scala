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
    SerializableMessage(data) match {
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
        case msg: CorrelatedRequest ⇒
          log.debug(s"Received $msg from ${sender()}")
          connection ! Write(msg.remoteRequest(sender()).toByteString)
        case msg: SerializableMessage ⇒
          log.debug(s"Writing $msg")
          connection ! Write(msg.toByteString)
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

trait CorrelatedRequest {
  def remoteRequest(replyTo: ActorRef): SerializableMessage
}

case class ClientAskLogin(username: String, password: String) extends CorrelatedRequest {
  override def remoteRequest(replyTo: ActorRef) = AskLogin(username, password, replyTo)
}

case class ClientAskParticipants(roomName: String) extends CorrelatedRequest {
  override def remoteRequest(replyTo: ActorRef) = AskParticipants(roomName, replyTo)
}
