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

import akka.actor.{ActorRef, ActorSystem}
import akka.event.Logging
import akka.util.ByteString
import reactivemongo.bson._
import reactivemongo.bson.DefaultBSONHandlers._
import reactivemongo.bson.buffer.{ArrayBSONBuffer, ArrayReadableBuffer, DefaultBufferHandler, WritableBuffer}

import scala.collection.mutable
import scalable.GlobalEnv


object MessageType extends Enumeration {
  type MessageType = Value
  val AskLoginType, LoginResultType, JoinType, JoinedType, LeaveChatType = Value
  val AskParticipantsType, ParticipantsType, ChatType, HistoryType = Value
}
import scalable.infrastructure.api.MessageType._

object ResultStatus extends Enumeration {
  type ResultStatus = Value
  val Ok, WrongPassword = Value
}
import scalable.infrastructure.api.ResultStatus._

object SerializableMessage {
  private lazy val log = Logging(GlobalEnv.system, getClass)
  type MessageDeserializer = BSONDocument => SerializableMessage

  private lazy val deserializers: mutable.Map[MessageType, MessageDeserializer] = mutable.Map()

  def registerDeserializer(messageType: MessageType, deserializer: BSONDocument ⇒ SerializableMessage) = {
    log.debug(s"Registering deserializer $messageType → ${deserializer.getClass}")
    deserializers += (messageType → deserializer)
  }

  def apply(byteString: ByteString)
           (implicit reader: BSONReader[BSONDocument, SerializableMessage],
            system: ActorSystem): SerializableMessage = {
    val bsonDocument = DefaultBufferHandler.readDocument(ArrayReadableBuffer(byteString.toArray)).get
    BSON.readDocument[SerializableMessage](bsonDocument)
  }

  implicit object MessageReader extends BSONDocumentReader[SerializableMessage] {
    def read(doc: BSONDocument): SerializableMessage = {
      val messageType = MessageType(doc.getAs[Int]("type").get)
      deserializers.get(messageType).map(des ⇒ des(doc))
        .getOrElse(sys.error(s"Unable to find deserializer for $messageType; Add type to preload list"))
    }
  }

  implicit object MessageWriter extends BSONDocumentWriter[SerializableMessage] {
    def write(message: SerializableMessage): BSONDocument =
      message.toBsonDocument
  }

  // because objects are instantiated lazily, unfortunately we need to make sure all
  // the message deserializers are eagerly created so they can register themselves
  // before an incoming message is received
  val preloadList = List(AskLogin, AskParticipants, Join, Joined, LeaveChat,
                         LoginResult, Participants, Chat, History)
}

import scalable.infrastructure.api.SerializableMessage.MessageDeserializer

sealed trait SerializableMessage {

  def toByteString(implicit writer: BSONWriter[SerializableMessage, BSONDocument]): ByteString = {
    val bson: BSONDocument = BSON.write(this)
    val buffer: WritableBuffer =  DefaultBufferHandler.writeDocument(bson, new ArrayBSONBuffer())
    val readableBuffer = buffer.toReadableBuffer()
    ByteString(readableBuffer.readArray(readableBuffer.readable()))
  }

  def toBsonDocument: BSONDocument
}

sealed trait MessageFactory extends MessageDeserializer {
  val typeCode: MessageType
  SerializableMessage.registerDeserializer(typeCode, this)
}

case class AskLogin(username: String, password: String, replyTo: ActorRef)
  extends SerializableMessage {
  import scalable.infrastructure.api.AskLogin._
  override def toBsonDocument: BSONDocument =
    BSONDocument(
    "type" → typeCode.id,
    "username" → username,
    "password" → password,
    "replyTo" → replyTo
  )
}

object AskLogin extends MessageFactory {
  override lazy val typeCode = AskLoginType
  override def apply(doc: BSONDocument): AskLogin = AskLogin(
    doc.getAs[String]("username").get,
    doc.getAs[String]("password").get,
    doc.getAs[ActorRef]("replyTo").get
  )
}

case class LoginResult(result: ResultStatus, username: String, replyTo: ActorRef)
extends SerializableMessage {
  import scalable.infrastructure.api.LoginResult._
  override def toBsonDocument: BSONDocument = BSONDocument(
    "type" → typeCode.id,
    "result" → result.id,
    "username" → username,
    "replyTo" → replyTo
  )
}

object LoginResult extends MessageFactory {
  override lazy val typeCode = LoginResultType
  override def apply(doc: BSONDocument): LoginResult = LoginResult(
    ResultStatus(doc.getAs[Int]("result").get),
    doc.getAs[String]("username").get,
    doc.getAs[ActorRef]("replyTo").get
  )
}

case class Join(username: String, roomName: String)
extends SerializableMessage {
  import scalable.infrastructure.api.Join._
  override def toBsonDocument: BSONDocument = BSONDocument(
    "type" → typeCode.id,
    "username" → username,
    "roomName" → roomName
  )
}

object Join extends MessageFactory {
  override lazy val typeCode = JoinType
  override def apply(doc: BSONDocument): Join = Join(
    doc.getAs[String]("username").get,
    doc.getAs[String]("roomName").get
  )
}

case class Joined(username: String, roomName: String)
extends SerializableMessage {
  import scalable.infrastructure.api.Joined._
  override def toBsonDocument: BSONDocument = BSONDocument(
    "type" → typeCode.id,
    "username" → username,
    "roomName" → roomName
  )
}

object Joined extends MessageFactory {
  override lazy val typeCode = JoinedType
  override def apply(doc: BSONDocument): Joined = Joined(
    doc.getAs[String]("username").get,
    doc.getAs[String]("roomName").get
  )
}

case class LeaveChat(username: String, roomName: String)
extends SerializableMessage {
  import scalable.infrastructure.api.LeaveChat._
  override def toBsonDocument: BSONDocument = BSONDocument(
    "type" → typeCode.id,
    "username" → username,
    "roomName" → roomName
  )
}

object LeaveChat extends MessageFactory {
  override lazy val typeCode = LeaveChatType
  override def apply(doc: BSONDocument): LeaveChat = LeaveChat(
    doc.getAs[String]("username").get,
    doc.getAs[String]("roomName").get
  )
}

case class AskParticipants(roomName: String, replyTo: ActorRef)
extends SerializableMessage {
  import scalable.infrastructure.api.AskParticipants._
  override def toBsonDocument: BSONDocument = BSONDocument(
    "type" → typeCode.id,
    "roomName" → roomName,
    "replyTo" → replyTo
  )
}

object AskParticipants extends MessageFactory {
  override lazy val typeCode = AskParticipantsType
  override def apply(doc: BSONDocument): AskParticipants = AskParticipants(
    doc.getAs[String]("roomName").get,
    doc.getAs[ActorRef]("replyTo").get
  )
}

case class Participants(roomName: String, participants: List[String], replyTo: ActorRef)
  extends SerializableMessage {
  import scalable.infrastructure.api.Participants._
  override def toBsonDocument: BSONDocument = BSONDocument(
    "type" → typeCode.id,
    "roomName" → roomName,
    "participants" → participants,
    "replyTo" → replyTo
  )
}

object Participants extends MessageFactory {
  override lazy val typeCode = ParticipantsType
  override def apply(doc: BSONDocument): Participants = Participants(
    doc.getAs[String]("roomName").get,
    doc.getAs[List[String]]("participants").toList.flatten,
    doc.getAs[ActorRef]("replyTo").get
  )
}

// The id is a server-generated time-based UUID, filled in by the server
case class Chat(id: Option[UUID], sender: String, roomName: String, htmlText: String)
  extends SerializableMessage {
  import scalable.infrastructure.api.Chat._

  override def toBsonDocument: BSONDocument = BSONDocument(
    "type" → typeCode.id,
    "id" → id,
    "sender" → sender,
    "roomName" → roomName,
    "htmlText" → htmlText
  )
}

object Chat extends MessageFactory {
  override lazy val typeCode: MessageType = ChatType

  override def apply(doc: BSONDocument): Chat = Chat(
    doc.getAs[UUID]("id"),
    doc.getAs[String]("sender").get,
    doc.getAs[String]("roomName").get,
    doc.getAs[String]("htmlText").get
  )

  implicit object ChatWriter extends BSONWriter[Chat, BSONDocument] {
    def write(message: Chat): BSONDocument =
      message.toBsonDocument
  }

  implicit object ChatReader extends BSONReader[BSONDocument, Chat] {
    override def read(bson: BSONDocument): Chat = Chat(bson)
  }

}

case class History(roomName: String, history: List[Chat])
  extends SerializableMessage {
  import scalable.infrastructure.api.History._

  override def toBsonDocument: BSONDocument = {
    BSONDocument(
      "type" → typeCode.id,
      "roomName" → roomName,
      "history" → BSONArray(history.map(BSON.writeDocument(_)))
    )
  }
}

object History extends MessageFactory {
  override lazy val typeCode: MessageType = HistoryType

  override def apply(doc: BSONDocument): History = {
    val history: BSONValue = doc.get("history").get
    History(
      doc.getAs[String]("roomName").get,
      history.asInstanceOf[BSONArray].values.toList.map{bsonValue ⇒
        bsonValue.asInstanceOf[BSONDocument].asOpt[Chat].get}
    )
  }
}
