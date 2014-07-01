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

import akka.actor.{ActorRefFactory, ActorSelection}
import scalable.client.tcp.TcpClient

import scalafx.Includes.jfxParent2sfx
import scalafx.scene.Parent
import scalafxml.core.{ControllerDependencyResolver, FXMLView}

/**
 * Client utilities.
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


}
