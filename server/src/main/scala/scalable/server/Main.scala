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

package scalable.server

import akka.actor._
import com.typesafe.config.ConfigFactory

import scalable.server.user.{ UserSessionPartition, UserSessionService }
import scalable.server.Configuration._

/**
 * Main entry points for the server.
 *
 * @author Eric Zoerner <a href="mailto:eric.zoerner@gmail.com">eric.zoerner@gmail.com</a>
 */
object Main {

  def main(args: Array[String]): Unit =
    if (args.isEmpty) {
      // for development and testing with an embedded cluster
      startupEmbeddedServiceNodes(Seq("2551", "2552", "0"))
      Front.main(Array.empty)
    }
    else
      // for production typically one port is passed into the command line
      startupEmbeddedServiceNodes(args)

  /**
   * Startup embedded actor systems.
   * For a production app, this would typically be just one system per VM.
   */
  def startupEmbeddedServiceNodes(ports: Seq[String]): Unit = {
    ports foreach { port ⇒
      // Override the configuration of the port when specified as program argument
      val config = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + port).
        withFallback(ConfigFactory.parseString(s"akka.cluster.roles = [$serviceRole]")).
        withFallback(ConfigFactory.load())

      val system = ActorSystem(AkkaSystemName, config)

      // Create one UserSessionService and UserSessionPartition per service node.
      // As specified in the configuration, (use-role = service)
      // when a message is sent to a UserSession, we send it to a UserSessionService
      // on any node in the cluster with the "service" role.
      // The UserSessionService uses a cluster-aware router to forward the message
      // to the "right" UserSessionPartition in the cluster for that user.
      system.actorOf(Props[UserSessionService], name = UserSessionServicePathElement)
      system.actorOf(Props[UserSessionPartition], name = UserSessionPartitionPathElement)
      // other services could be created here
    }
  }
}

/**
 * Handle startup for a front end cluster node,
 * i.e. one that provides connections to network clients.
 */
object Front {
  def main(args: Array[String]): Unit = {
    val system = ActorSystem(AkkaSystemName)
    val a = system.actorOf(ServerApp.props(s"/user/$UserSessionServicePathElement"), "app")
    system.actorOf(Props(classOf[Terminator], a, system), "terminator")
  }
}

class Terminator(ref: ActorRef, system: ActorSystem) extends Actor with ActorLogging {
  context watch ref
  def receive = {
    case Terminated(_) ⇒
      log.info("{} has terminated, shutting down system", ref.path)
      system.shutdown()
  }
}
