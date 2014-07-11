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

package scalable.server.tcp

import java.net.InetSocketAddress

import akka.actor.ActorRef
import akka.io.Tcp._
import org.specs2.mutable.Specification
import org.specs2.time.NoTimeConversions

import scala.concurrent.duration._
import scalable.infrastructure.api._
import scalable.server.{AkkaTestkitSpecs2Support, Configuration}


abstract class TcpClientAndServer extends AkkaTestkitSpecs2Support {

  def tcpService = system.actorOf(TcpService.props(self))
  def clientProps =  TestClient.props(new InetSocketAddress(Configuration.host, Configuration.portTcp), self)
  def clientAndServer: (ActorRef, ActorRef) = {
    tcpService ! NotifyOnBound()
    expectMsgType[Bound]
    val client = system.actorOf(clientProps)
    var server: ActorRef = null
    receiveWhile(2.seconds, 1.second, 2) {
      case msg: Connected ⇒ ()
      case NewConnection(conn) ⇒
        server = conn
        ()
    }
    (client, server)
  }
}

class TcpTest extends Specification with NoTimeConversions {
  sequential

  "TCP connection" should {
    "transfer a Login message from client to server" in new TcpClientAndServer {
      within(5.second) {
        val (tcpClient, _) = clientAndServer
        val msg = AskLogin("user","password", pickleActorRef(tcpClient))
        tcpClient ! msg.toByteString
        expectMsgType[AskLogin] must be equalTo msg
      }
    }

    "transfer a LoginResult message from server to client" in new TcpClientAndServer {
      within(5.second) {
        val (_, tcpServer) = clientAndServer
        tcpServer ! LoginResult(Ok(), "user", pickleActorRef(tcpServer))
        expectMsgType[LoginResult]
      }
    }
  }
}
