package scalable.server.tcp

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.io.{IO, Tcp}
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

    case b @ Bound(localAddr) =>
      log.debug(s"Received $b")
      localAddress = Some(localAddr)
      bindListener.fold(()){_ ! b}

    case NotifyOnBound() ⇒
      if (localAddress.isDefined)
        sender() ! Bound(localAddress.get)
      else
        bindListener = Some(sender())

    case Tcp.CommandFailed(_: Tcp.Bind) => context stop self
    case Tcp.Connected(remote, local) =>
      sender ! Tcp.Register(context.actorOf(TcpServer.props(sender(), listener)))
  }
}

case class NotifyOnBound()
