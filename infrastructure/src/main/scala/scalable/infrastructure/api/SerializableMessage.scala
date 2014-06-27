package scalable.infrastructure.api

import akka.actor.{ActorSystem, ActorRef}
import akka.util.ByteString
import reactivemongo.bson._
import reactivemongo.bson.buffer.{ArrayBSONBuffer, ArrayReadableBuffer, DefaultBufferHandler, WritableBuffer}


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

  def apply(byteString: ByteString)
           (implicit reader: BSONReader[BSONDocument, SerializableMessage[_]],
            system: ActorSystem): SerializableMessage[_] = {
    val bsonDocument = DefaultBufferHandler.readDocument(ArrayReadableBuffer(byteString.toArray)).get
    BSON.readDocument[SerializableMessage[_]](bsonDocument)
  }

  implicit object MessageReader extends BSONDocumentReader[SerializableMessage[_]] {
    def read(doc: BSONDocument): SerializableMessage[_] = {
      val messageType = MessageType(doc.getAs[Int]("type").get)
      messageType match {
        case AskLoginType ⇒ AskLogin(doc.getAs[String]("username").get,
                               doc.getAs[String]("password").get,
                               doc.getAs[ActorRef]("replyTo").get)
        case LoginResultType ⇒ LoginResult(ResultStatus(doc.getAs[Int]("result").get),
                                           doc.getAs[String]("username").get,
                                           doc.getAs[ActorRef]("replyTo").get)
        case JoinType ⇒ Join(doc.getAs[String]("username").get,
                             doc.getAs[String]("roomName").get)
        case JoinedType ⇒ Joined(doc.getAs[String]("username").get,
                                 doc.getAs[String]("roomName").get)
        case AskParticipantsType ⇒ AskParticipants(doc.getAs[String]("roomName").get,
                                                   doc.getAs[ActorRef]("replyTo").get)
        case ParticipantsType ⇒ Participants(doc.getAs[String]("roomName").get,
                                             doc.getAs[List[String]]("participants").toList.flatten,
                                             doc.getAs[ActorRef]("replyTo").get)
      }
    }
  }
}

trait SerializableMessage[T] {

  // TODO: Refactor to use one implicit  writer for all message types
  // allowing a message types as SerializableMessage to be written
  def toByteString(implicit writer: BSONWriter[T, BSONDocument]): ByteString = {
    val bson: BSONDocument = BSON.write[T, BSONDocument](this.asInstanceOf[T])
    val buffer: WritableBuffer =  DefaultBufferHandler.writeDocument(bson, new ArrayBSONBuffer())
    val readableBuffer = buffer.toReadableBuffer()
    ByteString(readableBuffer.readArray(readableBuffer.readable()))
  }
}

trait MessageFactory {
  val typeCode: MessageType
}

case class AskLogin(username: String, password: String, replyTo: ActorRef)
  extends SerializableMessage[AskLogin]

object AskLogin extends MessageFactory {
  val typeCode = AskLoginType

  implicit object AskLoginWriter extends BSONDocumentWriter[AskLogin] {
    def write(login: AskLogin): BSONDocument = BSONDocument(
      "type" → typeCode.id,
      "username" → login.username,
      "password" → login.password,
      "replyTo" → login.replyTo
    )
  }
}

case class LoginResult(result: ResultStatus, username: String, replyTo: ActorRef)
extends SerializableMessage[LoginResult]

object LoginResult extends MessageFactory {
  val typeCode = LoginResultType

  implicit object LoginResultWriter extends BSONDocumentWriter[LoginResult] {
    def write(loginResult: LoginResult): BSONDocument = BSONDocument(
      "type" → typeCode.id,
      "result" → loginResult.result.id,
      "username" → loginResult.username,
      "replyTo" → loginResult.replyTo
    )
  }
}

case class Join(username: String, roomName: String)
extends SerializableMessage[Join]

object Join extends MessageFactory {
  val typeCode = JoinType

  implicit object JoinWriter extends BSONDocumentWriter[Join] {
    def write(join: Join): BSONDocument= BSONDocument("type" → typeCode.id,
                                                      "username" → join.username,
                                                      "roomName" → join.roomName)
  }
}

case class Joined(username: String, roomName: String)
extends SerializableMessage[Joined]

object Joined extends MessageFactory {
  val typeCode = JoinedType

  implicit object JoinedWriter extends BSONDocumentWriter[Joined] {
    def write(joined: Joined): BSONDocument= BSONDocument("type" → typeCode.id,
                                                      "username" → joined.username,
                                                      "roomName" → joined.roomName)
  }
}

case class AskParticipants(roomName: String, replyTo: ActorRef)
extends SerializableMessage[AskParticipants]

object AskParticipants extends MessageFactory {
  val typeCode = AskParticipantsType

  implicit object AskParticipantsWriter extends BSONDocumentWriter[AskParticipants] {
    override def write(ap: AskParticipants): BSONDocument = BSONDocument("type" → typeCode.id,
                                                                         "roomName" → ap.roomName,
                                                                         "replyTo" → ap.replyTo)
  }
}

case class Participants(roomName: String, participants: List[String], replyTo: ActorRef)
  extends SerializableMessage[Participants]

object Participants extends MessageFactory {
  val typeCode = ParticipantsType

  implicit object ParticipantsWriter extends BSONDocumentWriter[Participants] {
    override def write(p: Participants): BSONDocument = BSONDocument("type" → typeCode.id,
                                                                     "roomName" → p.roomName,
                                                                     "participants" → p.participants,
                                                                     "replyTo" → p.replyTo)
  }
}


