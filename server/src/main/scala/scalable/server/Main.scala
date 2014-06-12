package scalable.server

import com.typesafe.config.ConfigFactory

/**
 * Main entry point for the server app.
 *
 * @author Eric Zoerner <a href="mailto:eric.zoerner@gmail.com">eric.zoerner@gmail.com</a>
 */
object Main extends App {
  akka.Main.main(Array(classOf[ServerApp].getName))
}


object Configuration {

  private val config = ConfigFactory.load
  config.checkValid(ConfigFactory.defaultReference)

  val host = config.getString("scalable.host")
  val portHttp = config.getInt("scalable.ports.http")
  val portTcp  = config.getInt("scalable.ports.tcp")
  val portWs   = config.getInt("scalable.ports.ws")
}
