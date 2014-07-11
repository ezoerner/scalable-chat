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
import javafx.event.EventHandler
import javafx.stage.WindowEvent

import akka.actor.ActorSystem
import akka.event.Logging

import scala.collection.SortedMap
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}
import scalable.client.chat.views.Browser
import scalable.client.chat.{ChatHandler, ChatListener}
import scalable.infrastructure.api._
import scalafx.Includes._
import scalafx.application.Platform
import scalafx.event.ActionEvent
import scalafx.scene.control._
import scalafx.scene.layout._
import scalafx.scene.text.Text
import scalafx.scene.web._
import scalafx.stage.Stage
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
    extends ChatListener with ChatController {
  private val log = Logging(actorSystem, this.getClass)
  private val RoomName = "Lobby"
  private val browser = new Browser("")
  private val htmlBuilder = new StringBuilder(Browser.getHtml(""))
  private var insertionIndexes = SortedMap[Long, Int]()
  private var bottomInsertionIndex = Browser.getHtml("").indexOf("</div>")
  private val dateFormat = DateFormat.getDateInstance
  private val timeFormat = DateFormat.getTimeInstance

  override def setStageAndSetupListeners(stage: Stage): Unit = {
    chatHandler.getInitialParticipants(RoomName).onComplete {
      case Success(participants) => Platform.runLater {
        log.info(s"initial participants=$participants")
        onlineListView.items.get() ++= participants
       ()
      }
      case Failure(t) => log.error(t, "Error while trying to get participants")
    }
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

    stage.setOnCloseRequest(new EventHandler[WindowEvent]() {
      override def handle(event: WindowEvent): Unit = chatHandler.leave(RoomName, username)
    })
  }

  override def joined(username: String): Unit = {
    log.debug(s"$username joined Lobby")
    Platform.runLater {
      if (onlineListView.items.get().add(username)) {
        onlineListView.items.get().sort()
      }
    }
  }

  override def left(username: String): Unit = {
    log.debug(s"$username left Lobby")
    Platform.runLater {
      onlineListView.items.get -= username
      ()
    }
  }

  override def receiveChat(id: MessageId, sender: String, htmlText: String): Unit = {
    updateHtmlBuilderWithNewContent(id.unixTimestamp, sender, htmlText)
    updateBrowser()
  }

  override def receiveHistory(history: List[Chat]): Unit = {
    history.foreach { case Chat(sender, _, htmlText, id) ⇒
      updateHtmlBuilderWithNewContent(id.unixTimestamp, sender, htmlText)
    }
    updateBrowser()
  }

  private def updateBrowser() = {
    browser.setContent(htmlBuilder.mkString)
  }

  def sendChat(event: ActionEvent) = {

      def extractNewContent(htmlString: String) = {
        val beginBody = htmlString.indexOf("<body")
        val begin = htmlString.indexOf('>', beginBody) + 1
        val end = htmlString.lastIndexOf("</body>")
        htmlString.substring(begin, end)
      }

    val html = extractNewContent(chatEditor.htmlText)
    log.debug(s"Send: $html")
    chatHandler.sendChat(RoomName, username, html)
  }

  private val headerFontStyle = s"""size="1" face="Courier" color="#1a3399""""
  private val headerFontStart = s"""<font $headerFontStyle>"""
  private val fontEnd = "</font>"
  private val headerStyle = """style="border-right:1px solid black;padding-right:5px;vertical-align:text-top;""""
  private val contentStyle = """style="padding-left:5px;""""
  private val HrStyle = "height: 12px;border: 0;box-shadow: inset 0 12px 12px -12px rgba(0,0,0,0.5);"

  def updateHtmlBuilderWithNewContent(timestamp: Long, sender: String, htmlText: String): Unit = {
    lazy val (r, g, b) = if (sender == username) (204, 255, 255) else (253, 246, 227)
    lazy val timeView = s"$headerFontStart${timeFormat.format(new Date(timestamp))}$fontEnd"
    lazy val dateView = s"$headerFontStart${dateFormat.format(new Date(timestamp))}$fontEnd"
    lazy val senderView = headerFontStart + sender + fontEnd

    def insertionIndex(stringToInsert: String): Int = {
      val indexDisplacement = stringToInsert.length
      if (insertionIndexes.lastOption.fold(true){ case (t,i) ⇒ timestamp >= t}) {
        val index = bottomInsertionIndex
        bottomInsertionIndex = index + indexDisplacement
        insertionIndexes = insertionIndexes + (timestamp → index)
        index
      } else {
        val pivotIndex = (insertionIndexes to timestamp).size
        val (beforeMap, afterMap) = insertionIndexes.splitAt(pivotIndex)
        assert(!afterMap.isEmpty)
        val insertionIndexIntoWeb = afterMap(0)
        val updatedAfterMap = afterMap.map{case (k,v) ⇒ k → (v + indexDisplacement)}
        insertionIndexes = (beforeMap + (timestamp → insertionIndexIntoWeb)) ++ updatedAfterMap
        insertionIndexIntoWeb
      }
    }

    def integrateNewContent(): Unit = {
        val divString = s"""<div><table>
                            |<colgroup><col style="background-color:rgb($r, $g, $b);"></colgroup>
                            |<tr><td $headerStyle>$senderView</td><td rowspan="3" $contentStyle>$htmlText</td></tr>
                            |<tr><td $headerStyle>$dateView</td></tr>
                            |<tr><td $headerStyle>$timeView</td></tr>
                            |</table><hr style="$HrStyle"/></div>""".stripMargin
        htmlBuilder.insert(insertionIndex(divString), divString)
      }

    val newHtmlText = integrateNewContent()
    log.debug(s"New HTML=$newHtmlText")
    newHtmlText
  }
}
