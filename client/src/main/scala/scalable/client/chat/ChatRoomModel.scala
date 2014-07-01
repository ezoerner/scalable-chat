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

package scalable.client.chat

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.util.Timeout
import scalable.client._
import scalable.client.tcp.ClientAskParticipants
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
    val futureResponse = (tcpClient ? ClientAskParticipants(roomName)).mapTo[Participants]
    online.clear()
    val participants = Await.result(futureResponse, 2.seconds)
    online.insertAll(0, participants.participants)
  }


}
