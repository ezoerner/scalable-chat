package scalable.client

import akka.actor._
import com.typesafe.config.ConfigFactory
import scalable.GlobalEnv

import scala.reflect.runtime.universe.typeOf
import scala.util.control.NonFatal
import scalafx.application.JFXApp
import scalafx.application.JFXApp.PrimaryStage
import scalafx.scene.Scene
import scalafxml.core.DependenciesByType

/**
 * Main entry point of client application.
 *
 * @author Eric Zoerner <a href="mailto:eric.zoerner@gmail.com">eric.zoerner@gmail.com</a>
 */

object Main extends JFXApp  {
  lazy val actorSystem = GlobalEnv.createActorSystem("Main")

  def dependencies = new DependenciesByType(Map(typeOf[ActorSystem] -> actorSystem))
  val root = loadFxmlFile("Login.fxml", dependencies)

  stage = new PrimaryStage() {
    title = "Login / Register"
    scene = new Scene(root)
  }

  override def main(args: Array[String]): Unit = {
    val root = try {
      actorSystem.actorOf(ClientApp.props, ClientApp.path)
    } catch {
      case NonFatal(e) ⇒ GlobalEnv.shutdownActorSystem(); throw e
    }
    actorSystem.actorOf(Terminator.props(root), Terminator.path)

    // do not exit the application when last window closes
    //Platform.implicitExit = false

    super.main(args)

    root ! PoisonPill
  }
}

object Configuration {

  private val config = ConfigFactory.load
  config.checkValid(ConfigFactory.defaultReference)

  val host = config.getString("scalable.host")
  val portTcp  = config.getInt("scalable.ports.tcp")
}

