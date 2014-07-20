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

import scalable.infrastructure.api.ResultStatus._
import scalable.infrastructure.api.{ AskLogin, LoginResult }

/**
 * State for a registered user (whether currently online or not).
 *
 * @author Eric Zoerner <a href="mailto:eric.zoerner@gmail.com">eric.zoerner@gmail.com</a>
 */
object UserSession {
  def props(login: AskLogin, connector: ActorRef) =
    Props(new UserSession(login.username, login.password, connector))

  def userSessionName(username: String) = s"user-$username"
}

class UserSession(val username: String, val password: String, var tcpConnector: ActorRef)
    extends Actor with ActorLogging {
  log.debug("Constructing UserSession")

  // For a newly created session, send LoginResult back to client for successful login
  tcpConnector ! LoginResult(Ok, username)
  loggedIn()

  override def receive: Receive = {
    case msg @ (login: AskLogin, connector: ActorRef) ⇒
      // login for existing session, verify password
      log.debug(s"Received $msg")
      assert(login.username == username)
      val resultStatus = if (login.password == password) {
        tcpConnector = connector
        loggedIn()
        Ok
      }
      else WrongPassword
      connector ! LoginResult(resultStatus, login.username)
  }

  private def loggedIn(): Unit = {
  }
}

