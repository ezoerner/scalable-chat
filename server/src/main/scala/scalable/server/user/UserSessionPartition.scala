package scalable.server.user

import akka.actor.{ Actor, ActorLogging, ActorRef }

import scala.collection.mutable
import scalable.infrastructure.api.AskLogin

/** A partition of UserSessions and routee of the UserSessionService.
  * This actor just forwards messages to UserSessions with this as the sender.
  * We don't use a router for this because the single-threaded guarantee of an actor
  * helps with managing the map of user sessions.
  *
  * @author Eric Zoerner <a href="mailto:eric.zoerner@gmail.com">eric.zoerner@gmail.com</a>
  */
class UserSessionPartition extends Actor with ActorLogging {

  var userSessions: mutable.Map[String, ActorRef] = mutable.Map.empty

  private def createSession(login: AskLogin): ActorRef =
    context.actorOf(UserSession.props(login), login.username)

  override def receive: Receive = {
    case msg @ (login: AskLogin, connector: ActorRef) ⇒
      log.info(s"received login user ${login.username}")
      val userSession = userSessions.getOrElseUpdate(login.username, createSession(login))
      userSession ! msg
    case other ⇒ log.warning(s"Received unexpected message $other")
  }
}
