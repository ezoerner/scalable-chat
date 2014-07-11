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

import akka.util.ByteString

import scala.pickling._
import scala.pickling.binary._

import com.datastax.driver.core.utils.UUIDs

sealed trait ResultStatus
case class Ok() extends ResultStatus
case class WrongPassword() extends ResultStatus

case class MessageId(msb: Long, lsb: Long) {
  def timeBasedUuid: UUID = new UUID(msb, lsb)
  def unixTimestamp: Long = UUIDs.unixTimestamp(timeBasedUuid)
}

object MessageId {
  def apply(uuid: UUID): MessageId = MessageId(uuid.getMostSignificantBits, uuid.getLeastSignificantBits)
}

object SerializableMessage {
  def apply(byteString: ByteString): SerializableMessage = byteString.toArray.unpickle[SerializableMessage]
}

sealed trait SerializableMessage {
  def toByteString: ByteString = ByteString(this.pickle.value)
}

case class AskLogin(username: String, password: String, replyTo: ByteString) extends SerializableMessage
case class LoginResult(result: ResultStatus, username: String, replyTo: ByteString) extends SerializableMessage
case class Join(username: String, roomName: String) extends SerializableMessage
case class Joined(username: String, roomName: String) extends SerializableMessage
case class LeaveChat(username: String, roomName: String) extends SerializableMessage
case class AskParticipants(roomName: String, replyTo: ByteString) extends SerializableMessage
case class Participants(roomName: String, participants: List[String], replyTo: ByteString) extends SerializableMessage

// The id is a server-generated time-based UUID, filled in by the server
case class Chat(sender: String, roomName: String, htmlText: String, id: MessageId = null)
  extends SerializableMessage

case class History(roomName: String, history: List[Chat]) extends SerializableMessage