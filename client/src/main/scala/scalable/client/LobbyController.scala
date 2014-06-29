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

import java.text.DateFormat
import java.util.{Date, UUID}
import javafx.beans.value.{ChangeListener, ObservableValue}

import akka.actor.ActorSystem
import akka.event.Logging

import scalable.client.chat.views.Browser
import scalable.client.chat.{ChatHandler, ChatListener, ChatRoomModel}
import scalafx.application.Platform
import scalafx.event.ActionEvent
import scalafx.scene.control._
import scalafx.scene.layout._
import scalafx.scene.text.Text
import scalafx.scene.web._
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
                      private val onlineListView: ListView[String],
                      private val chatEditor: HTMLEditor,
                      private val sendChat: Button,
                      private val webViewParent: AnchorPane,
                      private val chatScrollPane: ScrollPane)
    extends ChatListener {
  private val log = Logging(actorSystem, this.getClass)
  private val RoomName = "Lobby"
  private val browser = new Browser("")
  private val htmlBuilder = new StringBuilder(browser.getHtml(""))
  private var insertionIndex = browser.getHtml("").indexOf("</div>")
  private val format = DateFormat.getDateTimeInstance
  private val chatRoom = new ChatRoomModel(RoomName, actorSystem)

  onlineListView.items = chatRoom.online

  usernameText.text = username

  assert(onlineTitledPane != null)
  accordion.expandedPane = onlineTitledPane


  webViewParent.children.add(browser)
  webViewParent.width.addListener(new ChangeListener[Any] {
    override def changed(observable: ObservableValue[_], oldValue: Any, newValue: Any): Unit = {
      browser.setPrefWidth(newValue.asInstanceOf[Double])
    }
  })

  browser.heightProperty().addListener(new ChangeListener[Any] {
    override def changed(observable: ObservableValue[_], oldValue: Any, newValue: Any): Unit = {
      chatScrollPane.vvalue = newValue.asInstanceOf[Double]
    }
  })

  Platform.runLater {
    chatHandler.addChatListener(this, RoomName)
    chatHandler.join(username, RoomName)
  }

  override def joined(username: String): Unit = {
    log.debug(s"$username joined Lobby")
    Platform.runLater {
      if (chatRoom.online.add(username)) {
        chatRoom.online.sort()
      }
    }
  }

  override def receiveChat(id: UUID, sender: String, htmlText: String): Unit = {
    // TODO: consider using eaio.uuid library to insert message in sorted order
    // TODO: based directly on the time-based UUID
    appendToChatGrid(unixTimestamp(id), sender, htmlText)
  }

  def sendChat(event: ActionEvent) = {

    def extractNewContent(htmlString: String) = {
      val beginBody = htmlString.indexOf("<body")
      val begin = htmlString.indexOf('>', beginBody) + 1
      val end = htmlString.lastIndexOf("</body>")
      htmlString.substring(begin, end)
    }

    val html = extractNewContent(chatEditor.htmlText)
    log.debug(s"HTML: $html")
    chatRoom.sendChat(username, html)
  }

  def appendToChatGrid(timestamp: Long, sender: String, htmlText: String) = {
      def header = {
        val (r,g,b) = if (sender == username) (204, 255, 255) else (253, 246, 227)
        s"""<p><font size="2" face="Courier" style="background-color: rgb($r, $g, $b);" color="#1a3399">
           |$sender | ${format.format(new Date(timestamp))}</font></p>""".stripMargin
      }

      def integrateNewContent(content: String): String = {
        // TODO: insert in correct order based on timestamp
        val divString = "<div>" + header + content + s"""<hr style="$HrStyle"/></div>"""
        htmlBuilder.insert(insertionIndex, divString)
        insertionIndex = insertionIndex + divString.length
        htmlBuilder.mkString
      }

    val newHtmlText = integrateNewContent(htmlText)
    log.debug(s"New HTML=$newHtmlText")
    browser.setContent(newHtmlText)
  }

  private val HrStyle = "height: 12px;border: 0;box-shadow: inset 0 12px 12px -12px rgba(0,0,0,0.5);"
}