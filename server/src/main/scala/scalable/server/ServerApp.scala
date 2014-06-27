package scalable.server

import akka.actor._
import scalable.GlobalEnv
import scalable.infrastructure.api.{AskLogin, AskParticipants, Join}
import scalable.server.chat.ChatRoom
import scalable.server.tcp.{NewConnection, TcpService}

import scala.util.Try

/**
 * Root actor of the server application.
 *
 * @author Eric Zoerner <a href="mailto:eric.zoerner@gmail.com">eric.zoerner@gmail.com</a>
 */
object ServerApp {
  val path = "/user/app"
}

class ServerApp extends Actor with ActorLogging {
  log.debug(s"Main Actor path=${self.path.toStringWithoutAddress}")
  context.actorOf(TcpService.props(self), "tcpService")
  lazy val lobbyChatRoom = context.actorOf(ChatRoom.props("Lobby"), "lobby")


  private def login(login: ServerLogin): Unit = {
    // We use dead simple authentication logic.
    // if a session already exists for this user then the passwords will be checked.
    // if the passwords are the same then the user rejoins the session
    // otherwise the username is already taken and login is rejected

    def createSession(login: ServerLogin): Try[ActorRef] = {
      Try(context.actorOf(UserSession.props(login), login.username))
    }

    val newSession = createSession(login)
    newSession.recover {
      case _: InvalidActorNameException ⇒
        // existing session will verify password and send back a LoginResult
        context.actorSelection(login.username) ! login
      case ex ⇒ throw ex
    }
  }


  override def receive = {
    case msg: AskLogin ⇒
      log.debug(s"Received $msg")
      login(ServerLogin(msg.username, msg.password, msg.replyTo, sender()))
    case msg: NewConnection ⇒
      // not interested
      log.debug(s"Received $msg")
    case msg: Join ⇒
      assert(msg.roomName == "Lobby") // "Lobby" is currently the only top-level chat room
      lobbyChatRoom ! ServerJoin(msg.username, msg.roomName, sender())
    case msg: AskParticipants ⇒
      assert(msg.roomName == "Lobby")
      lobbyChatRoom ! ServerAskParticipants(msg.roomName, msg.replyTo, sender())
    case msg ⇒ log.error(s"Received unexpected message: $msg")
  }
}

case class ServerLogin(username: String, password: String, replyTo: ActorRef, connector: ActorRef)
case class ServerJoin(username: String, roomName: String, connector: ActorRef)
case class ServerAskParticipants(roomName: String, replyTo: ActorRef, connector: ActorRef)
