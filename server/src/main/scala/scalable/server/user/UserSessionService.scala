package scalable.server.user

import akka.actor.{ Actor, ActorRef }
import akka.routing.ConsistentHashingRouter.ConsistentHashableEnvelope
import akka.routing.FromConfig

import scalable.infrastructure.api.AskLogin
import scalable.server.Configuration

/** Service that provides access to UserSessions.
  * Uses a cluster-aware router to route to UserSessionPartitions.
  * @see UserSessionPartition
  * @author Eric Zoerner <a href="mailto:eric.zoerner@gmail.com">eric.zoerner@gmail.com</a>
  */
class UserSessionService extends Actor {

  val partitionRouter = context.actorOf(
    FromConfig.props(),
    name = Configuration.UserSessionPartitionRouter
  )

  override def receive: Receive = {
    case msg @ (login: AskLogin, connector: ActorRef) ⇒
      partitionRouter ! ConsistentHashableEnvelope(msg, login.username)
  }
}
