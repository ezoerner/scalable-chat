package scalable.server

import com.typesafe.config.ConfigFactory

/**
 * Helper object for accessing the application configuration.
 *
 * @author Eric Zoerner <a href="mailto:eric.zoerner@gmail.com">eric.zoerner@gmail.com</a>
 */
object Configuration {
  val UserSessionServicePathElement = "userSessionService"
  val UserSessionPartitionPathElement = "userSessionPartition"
  val UserSessionPartitionRouter = "partitionRouter"
  val AkkaSystemName = "ClusterSystem"

  private val config = ConfigFactory.load
  config.checkValid(ConfigFactory.defaultReference)

  val host = config.getString("scalable.host")
  val portTcp = config.getInt("scalable.ports.tcp")
  val serviceRole = config.getString(s"akka.actor.deployment./$UserSessionServicePathElement/$UserSessionPartitionRouter.cluster.use-role")
}
