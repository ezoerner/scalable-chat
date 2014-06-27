package scalable.server

import akka.actor._
import com.typesafe.config.ConfigFactory

import scalable.GlobalEnv

/**
 * Main entry point for the server app.
 *
 * @author Eric Zoerner <a href="mailto:eric.zoerner@gmail.com">eric.zoerner@gmail.com</a>
 */
object Main {

  def main(args: Array[String]): Unit = {
    val system = GlobalEnv.createActorSystem("Main")
    val a = system.actorOf(Props[ServerApp], "app")
    system.actorOf(Props(classOf[Terminator], a), "terminator")
  }

  class Terminator(ref: ActorRef) extends Actor with ActorLogging {
    context watch ref
    def receive = {
      case Terminated(_) =>
        log.info("{} has terminated, shutting down system", ref.path)
        GlobalEnv.shutdownActorSystem()
    }
  }
}


object Configuration {

  private val config = ConfigFactory.load
  config.checkValid(ConfigFactory.defaultReference)

  val host = config.getString("scalable.host")
  val portHttp = config.getInt("scalable.ports.http")
  val portTcp  = config.getInt("scalable.ports.tcp")
  val portWs   = config.getInt("scalable.ports.ws")
}
