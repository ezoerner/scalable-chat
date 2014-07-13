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

package scalable.infrastructure.api

import java.util.UUID

import akka.actor.ActorSystem
import akka.serialization.SerializationExtension
import akka.util.ByteString

object ResultStatus extends Enumeration {
  type ResultStatus = Value
  val Ok, WrongPassword = Value
}
import scalable.infrastructure.api.ResultStatus._

object SerializableMessage {
  def apply(byteString: ByteString)(implicit system: ActorSystem): SerializableMessage = {
    val serialization = SerializationExtension(system)
    serialization.deserialize(byteString.toArray, classOf[SerializableMessage])
      .recover { case e ⇒ throw e }
      .get
  }
}

sealed trait SerializableMessage extends Serializable {
  def toByteString(implicit system: ActorSystem): ByteString = {
    val serialization = SerializationExtension(system)
    ByteString(serialization.serialize(this)
      .recover { case e ⇒ throw e }
      .get)
  }
}

case class AskLogin(username: String, password: String)
  extends SerializableMessage

case class LoginResult(result: ResultStatus, username: String)
  extends SerializableMessage

case class Join(username: String, roomName: String)
  extends SerializableMessage

case class LeaveChat(username: String, roomName: String)
  extends SerializableMessage

// The id is a server-generated time-based UUID, filled in by the server
case class Chat(id: Option[UUID], sender: String, roomName: String, htmlText: String)
  extends SerializableMessage

case class RoomInfo(roomName: String, history: List[Chat], participants: List[String])
  extends SerializableMessage