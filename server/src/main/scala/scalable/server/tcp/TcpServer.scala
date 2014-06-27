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
