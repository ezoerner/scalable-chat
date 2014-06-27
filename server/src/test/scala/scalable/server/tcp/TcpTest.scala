package scalable.server.tcp

import java.net.InetSocketAddress

import akka.actor.ActorRef
import akka.io.Tcp._
import scalable.infrastructure.api.{AskLogin, LoginResult, ResultStatus}
import scalable.server.{AkkaTestkitSpecs2Support, Configuration}
import org.specs2.mutable.Specification
import org.specs2.time.NoTimeConversions

import scala.concurrent.duration._


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
        val msg = AskLogin("user","password", tcpClient)
        tcpClient ! msg.toByteString
        expectMsgType[AskLogin] must be equalTo msg
      }
    }

    "transfer a LoginResult message from server to client" in new TcpClientAndServer {
      within(5.second) {
        val (_, tcpServer) = clientAndServer
        tcpServer ! LoginResult(ResultStatus.Ok, "user", tcpServer)
        expectMsgType[LoginResult]
      }
    }
  }
}
