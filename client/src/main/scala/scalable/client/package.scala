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

package scalable

import java.io.IOException
import java.util.{ Calendar, TimeZone, UUID }

import akka.actor.{ ActorRefFactory, ActorSelection }

import scalable.client.tcp.TcpClient
import scalafx.Includes._
import scalafx.scene.Parent
import scalafxml.core.{ ControllerDependencyResolver, FXMLView }

/** Client utilities.
  *
  * @author Eric Zoerner <a href="mailto:eric.zoerner@gmail.com">eric.zoerner@gmail.com</a>
  */
package object client {

  def loadFxmlFile(resourceName: String, dependencies: ControllerDependencyResolver): Parent = {
      def resource = getClass.getResource(resourceName)
    if (resource == null) {
      throw new IOException(s"Cannot load resource: $resourceName")
    }

    FXMLView(resource, dependencies)
  }

  // selecting actors
  def tcpClientSelection(system: ActorRefFactory): ActorSelection = system.actorSelection(s"/user/${ClientApp.path}/${TcpClient.path}")
  def appSupervisorSelection(system: ActorRefFactory): ActorSelection = system.actorSelection(s"/user/${ClientApp.path}")

  // some utilities for working with time-based UUIDs
  private lazy val startEpoch = {
    val c: Calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT-0"))
    c.set(Calendar.YEAR, 1582)
    c.set(Calendar.MONTH, Calendar.OCTOBER)
    c.set(Calendar.DAY_OF_MONTH, 15)
    c.set(Calendar.HOUR_OF_DAY, 0)
    c.set(Calendar.MINUTE, 0)
    c.set(Calendar.SECOND, 0)
    c.set(Calendar.MILLISECOND, 0)
    c.getTimeInMillis
  }

  def unixTimestamp(uuid: UUID): Long = {
    require(uuid.version == 1)
    val timestamp: Long = uuid.timestamp
    (timestamp / 10000) + startEpoch
  }

  def nodeLookup[T](parent: javafx.scene.Node, clazz: Class[T]): Option[T] = {

      def seqLookup(nodes: List[javafx.scene.Node]): Option[T] = {
        nodes match {
          case node :: _ if node.getClass.isAssignableFrom(clazz) ⇒ Some(node.asInstanceOf[T])
          case (node: javafx.scene.Parent) :: rest ⇒ seqLookup(node.getChildrenUnmodifiable.toList ::: rest)
          case _ :: rest ⇒ seqLookup(rest)
          case Nil ⇒ None
        }
      }

    seqLookup(List(parent))
  }
}
