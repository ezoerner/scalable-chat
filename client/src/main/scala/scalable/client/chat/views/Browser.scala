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

package scalable.client.chat.views

import java.util
import javafx.beans.value.{ ObservableValue, ChangeListener }
import javafx.collections.ListChangeListener
import javafx.geometry.{ VPos, HPos, Insets }
import javafx.scene.Node
import javafx.scene.layout.Region
import javafx.scene.web.WebView
import javafx.concurrent.Worker.State
import scala.collection.JavaConverters._
import scalafx.application.Platform

/**
 * Code adapted from <a href="https://github.com/frtj/javafx_examples">github.com/frtj/javafx_examples</a>
 *
 * @author Eric Zoerner <a href="mailto:eric.zoerner@gmail.com">eric.zoerner@gmail.com</a>
 */
object Browser {
  private val ContentId = "browser_content"
  def getHtml(content: String): String = {
    "<html><body>" + "<div id=\"" + ContentId + "\">" + content + "</div>" + "</body></html>"
  }
}
import scalable.client.chat.views.Browser._

class Browser(content: String) extends Region {
  val webView = new WebView()
  val webEngine = webView.getEngine

  webView.setPrefHeight(5)
  setPadding(new Insets(20))

  widthProperty().addListener(new ChangeListener[Any] {
    override def changed(observable: ObservableValue[_], oldValue: Any, newValue: Any): Unit = {
      val width = newValue.asInstanceOf[Double]
      webView.setPrefWidth(width)
      adjustHeight()
    }
  })

  webEngine.getLoadWorker.stateProperty.addListener(new ChangeListener[State] {
    override def changed(arg0: ObservableValue[_ <: State], oldState: State, newState: State): Unit =
      if (newState == State.SUCCEEDED)
        adjustHeight()
  })

  // http://stackoverflow.com/questions/11206942/how-to-hide-scrollbars-in-the-javafx-webview
  webView.getChildrenUnmodifiable.addListener(new ListChangeListener[Node] {
    def onChanged(change: ListChangeListener.Change[_ <: Node]) = {
      val scrolls: util.Set[Node] = webView.lookupAll(".scroll-bar")
      for (scroll ← scrolls.asScala) {
        scroll.setVisible(false)
      }
    }
  })

  setContent(content)
  getChildren.add(webView)

  def setContent(content: String) = {
    Platform.runLater {
      webEngine.loadContent(getHtml(content))
      Platform.runLater(adjustHeight())
    }
  }

  protected override def layoutChildren() = {
    val w: Double = getWidth
    val h: Double = getHeight
    layoutInArea(webView, 0, 0, w, h, 0, HPos.CENTER, VPos.CENTER)
  }

  private def adjustHeight(): Unit = {
    Platform.runLater {
      val result: Any = webEngine.executeScript("var e = document.getElementById('" + ContentId + "');" +
        "e ? e.offsetHeight : null")
      result match {
        case i: Integer ⇒
          var height = i.toDouble
          height = height + 20
          webView.setPrefHeight(height)
        case _ ⇒
      }
    }
  }
}