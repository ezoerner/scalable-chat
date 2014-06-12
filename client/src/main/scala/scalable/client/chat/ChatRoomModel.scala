package scalable.client.chat

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.util.Timeout
import scalable.client._
import scalable.client.tcp.AskParticipants1
import scalable.infrastructure.api.Participants

import scala.concurrent.Await
import scala.concurrent.duration._
import scalafx.collections.ObservableBuffer

/**
 * Data model for a chat room.
 *
 * @author Eric Zoerner <a href="mailto:eric.zoerner@gmail.com">eric.zoerner@gmail.com</a>
 */
class ChatRoomModel(val roomName: String, private val system: ActorSystem) {
  private implicit val timeout: Timeout = 2.second

  val online = ObservableBuffer[String]()

  def initialize() = {
    val tcpClient = tcpClientSelection(system)
    val futureResponse = (tcpClient ? AskParticipants1(roomName)).mapTo[Participants]
    online.clear()
    val participants = Await.result(futureResponse, 2.seconds)
    online.insertAll(0, participants.participants)
  }


}
