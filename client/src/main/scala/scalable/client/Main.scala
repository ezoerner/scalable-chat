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

package scalable.client

import javafx.scene.Parent
import javafx.{ scene ⇒ jfxs }

import akka.actor._
import com.typesafe.config.ConfigFactory

import scala.reflect.runtime.universe.typeOf
import scala.util.control.NonFatal
import scalable.client.login.LoginListener
import scalable.messaging.api.ResultStatus._
import scalafx.Includes._
import scalafx.application.JFXApp
import scalafx.application.JFXApp.PrimaryStage
import scalafx.scene.Scene
import scalafxml.core.{ DependenciesByType, FXMLLoader }

/** Main entry point of client application.
  *
  * @author Eric Zoerner <a href="mailto:eric.zoerner@gmail.com">eric.zoerner@gmail.com</a>
  */

class Main extends JFXApp {
  lazy val actorSystem = ActorSystem("Main")

  def dependencies = new DependenciesByType(Map(typeOf[ActorSystem] -> actorSystem))

  val loader: FXMLLoader = new FXMLLoader(getClass.getResource("Login.fxml"), dependencies)
  val root: Parent = loader.load()
  val loginListener: LoginListener = loader.getController()
  require(loginListener != null)

  stage = new PrimaryStage() {
    title = "Login / Register"
    scene = new Scene(root)
  }

  loginListener.setLoginStage(stage)

  val rootActor = try {
    actorSystem.actorOf(ClientApp.props(loginListener), ClientApp.path)
  }
  catch {
    case NonFatal(e) ⇒
      actorSystem.shutdown()
      throw e
  }
  actorSystem.actorOf(Terminator.props(rootActor), Terminator.path)

  override def stopApp() = {
    rootActor ! PoisonPill
  }
}

object Configuration {

  private val config = ConfigFactory.load
  config.checkValid(ConfigFactory.defaultReference)

  val host = config.getString("scalable.host")
  val portTcp = config.getInt("scalable.ports.tcp")
}

