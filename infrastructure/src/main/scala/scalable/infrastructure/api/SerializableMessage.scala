package scalable.infrastructure.api

import akka.actor.{ActorSystem, ActorRef}
import akka.event.Logging
import akka.util.ByteString
import reactivemongo.bson._
import reactivemongo.bson.buffer.{ArrayBSONBuffer, ArrayReadableBuffer, DefaultBufferHandler, WritableBuffer}
import scala.collection.mutable
import scalable.GlobalEnv


object MessageType extends Enumeration {
  type MessageType = Value
  val AskLoginType, LoginResultType, JoinType, JoinedType, AskParticipantsType, ParticipantsType = Value
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
    log.debug(s"Registering deserializer $deserializer")
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
        .getOrElse(sys.error(s"Unable to find deserializer for $messageType"))
    }
  }

  implicit object MessageWriter extends BSONDocumentWriter[SerializableMessage] {
    def write(message: SerializableMessage): BSONDocument =
      message.toBsonDocument
  }

  // because objects are instantiated lazily, unfortunately we need to make sure all
  // the message deserializers are eagerly created so they can register themselves
  // before an incoming message is received
  (AskLogin, AskParticipants, Join, Joined, LoginResult, Participants)
}

import SerializableMessage.MessageDeserializer

trait SerializableMessage {

  def toByteString(implicit writer: BSONWriter[SerializableMessage, BSONDocument]): ByteString = {
    val bson: BSONDocument = BSON.write(this)
    val buffer: WritableBuffer =  DefaultBufferHandler.writeDocument(bson, new ArrayBSONBuffer())
    val readableBuffer = buffer.toReadableBuffer()
    ByteString(readableBuffer.readArray(readableBuffer.readable()))
  }

  def toBsonDocument: BSONDocument
}

trait MessageFactory extends MessageDeserializer {
  val typeCode: MessageType
  SerializableMessage.registerDeserializer(typeCode, this)
}

case class AskLogin(username: String, password: String, replyTo: ActorRef)
  extends SerializableMessage {
  import AskLogin._
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
  import LoginResult._
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
  import Join._
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
  import Joined._
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

case class AskParticipants(roomName: String, replyTo: ActorRef)
extends SerializableMessage {
  import AskParticipants._
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
  import Participants._
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


