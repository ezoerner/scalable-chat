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
