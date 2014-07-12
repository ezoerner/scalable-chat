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
import scalable.infrastructure.api.LoginResult
import scalable.infrastructure.api.ResultStatus._

/**
 * State for a registered user (whether currently online or not).
 *
 * @author Eric Zoerner <a href="mailto:eric.zoerner@gmail.com">eric.zoerner@gmail.com</a>
 */
object UserSession {
  def props(login: ServerLogin) = Props(new UserSession(login))
  def userSessionName(username: String) = s"user/$username"
}

class UserSession(login: ServerLogin) extends Actor with ActorLogging {
  val username = login.username
  val password = login.password
  var tcpConnector = login.connector

  // For a newly created session, send LoginResult back to client for successful login
  tcpConnector ! LoginResult(Ok, login.username, login.replyTo)
  loggedIn()

  override def receive: Receive = {
    case msg: ServerLogin ⇒
      // login for existing session, verify password
      log.debug(s"Received $msg")
      assert(msg.username == login.username)
      val resultStatus = if (msg.password == login.password) {
        tcpConnector = msg.connector
        loggedIn()
        Ok
      }
      else WrongPassword
      msg.connector ! LoginResult(resultStatus, msg.username, msg.replyTo)
  }

  private def loggedIn(): Unit = {
  }
}

