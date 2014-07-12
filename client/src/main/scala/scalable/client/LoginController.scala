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

import akka.actor.ActorSystem
import akka.event.Logging
import akka.pattern.{ AskTimeoutException, ask }
import akka.util.Timeout
import scalable.client.tcp.ClientAskLogin
import scalable.infrastructure.api.LoginResult
import scalable.infrastructure.api.ResultStatus._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scalafx.application.Platform
import scalafx.event.ActionEvent
import scalafx.scene.control.TextField
import scalafx.scene.layout.GridPane
import scalafx.scene.text.Text
import scalafxml.core.macros.sfxml

/**
 * Handles Login operations.
 *
 * @author Eric Zoerner <a href="mailto:eric.zoerner@gmail.com">eric.zoerner@gmail.com</a>
 */

@sfxml
class LoginController(private val usernameField: TextField,
                      private val passwordField: TextField,
                      private val failedText: Text,
                      private val timedOutText: Text,
                      private val root: GridPane,
                      private val actorSystem: ActorSystem) {
  private val log = Logging(actorSystem, this.getClass)
  private implicit val timeout: Timeout = 5.seconds

  def onKeyTyped(): Unit = {
    failedText.visible.value = false
    timedOutText.visible.value = false
  }

  def login(event: ActionEvent) = {
    timedOutText.visible.value = false

    val tcpClient = tcpClientSelection(actorSystem)
    val appSupervisor = appSupervisorSelection(actorSystem)

    log.debug("Asking the tcpClient with Login")
    val futureResponse =
      (tcpClient ? ClientAskLogin(usernameField.text.value, passwordField.text.value)).mapTo[LoginResult]

    futureResponse.onSuccess {
      case LoginResult(Ok, username, _) if username == usernameField.text.value ⇒
        log.debug("Successful Login")
        appSupervisor ! OpenLobby(username)
      // opening a new PrimaryStage will reuse this same window
      case LoginResult(status, username, _) ⇒
        Platform.runLater(failedText.visible.value = true)
        log.error(s"Unsuccessful login: $status, $username")
      case msg ⇒
        log.error(s"Received unknown message: $msg")
    }

    futureResponse.recover {
      case ex: AskTimeoutException ⇒
        log.error("Unsuccessful login: Timed Out")
        Platform.runLater(timedOutText.visible.value = true)
    }
  }

  def exit() = {
    Platform.exit()
  }
}
