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
