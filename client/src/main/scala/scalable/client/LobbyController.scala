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
import scalable.client.chat.{ChatHandler, ChatListener, ChatRoomModel}

import scalafx.application.Platform
import scalafx.scene.control.{Accordion, ListView, TitledPane}
import scalafx.scene.text.Text
import scalafxml.core.macros.sfxml

/**
 * Controller for Lobby window.
 *
 * @author Eric Zoerner <a href="mailto:eric.zoerner@gmail.com">eric.zoerner@gmail.com</a>
 */
@sfxml
class LobbyController(private val onlineTitledPane: TitledPane,
                      private val accordion: Accordion,
                      private val actorSystem: ActorSystem,
                      private val chatHandler: ChatHandler,
                      private val usernameText: Text,
                      private val username: String,
                      private val onlineListView: ListView[String])
    extends ChatListener {
  private val log = Logging(actorSystem, this.getClass)
  private val RoomName = "Lobby"
  private val chatRoom = new ChatRoomModel(RoomName, actorSystem)
  chatRoom.initialize()
  onlineListView.items = chatRoom.online

  usernameText.text = username

  assert(onlineTitledPane != null)
  accordion.expandedPane = onlineTitledPane

  Platform.runLater {
    chatHandler.addChatListener(this, RoomName)
    chatHandler.join(username, RoomName)
  }

  override def joined(username: String, roomName: String): Unit = {
    assert(roomName == RoomName)
    log.debug(s"$username joined Lobby")
    Platform.runLater {
      if (chatRoom.online.add(username)) {
        chatRoom.online.sort()
      }
    }
  }
}
