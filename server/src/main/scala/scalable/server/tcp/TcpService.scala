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

import akka.actor.{ Actor, ActorLogging, ActorRef, Props }
import akka.io.{ IO, Tcp }
import scalable.server.Configuration

object TcpService {
  def props(listener: ActorRef) = Props(new TcpService(listener))
}

class TcpService(private val listener: ActorRef) extends Actor with ActorLogging {

  import akka.io.Tcp._
  import context.system

  IO(Tcp) ! Bind(self, new InetSocketAddress(Configuration.host, Configuration.portTcp))
  log.debug(s"Binding: ${Configuration.host}, ${Configuration.portTcp}")

  private var bindListener: Option[ActorRef] = None
  private var localAddress: Option[InetSocketAddress] = None

  override def receive = {

    case b @ Bound(localAddr) ⇒
      log.info(s"Successfully $b")
      localAddress = Some(localAddr)
      bindListener.fold(()) { _ ! b }

    case NotifyOnBound() ⇒
      if (localAddress.isDefined)
        sender() ! Bound(localAddress.get)
      else
        bindListener = Some(sender())

    case Tcp.CommandFailed(_: Tcp.Bind) ⇒ context stop self
    case Tcp.Connected(remote, local) ⇒
      sender ! Tcp.Register(context.actorOf(TcpServer.props(sender(), listener)))
  }
}

case class NotifyOnBound()
