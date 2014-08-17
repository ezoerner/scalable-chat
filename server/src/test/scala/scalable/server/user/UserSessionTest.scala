package scalable.server.user

import akka.testkit.TestProbe
import org.specs2.time.NoTimeConversions
import org.specs2.mutable.Specification

import scalable.infrastructure.api.{ LoginResult, AskLogin }
import scalable.server.AkkaTestkitSpecs2Support
import scala.concurrent.duration._
import scalable.infrastructure.api.ResultStatus._

/**
 * Tests for UserSessions.
 *
 * @author Eric Zoerner <a href="mailto:eric.zoerner@gmail.com">eric.zoerner@gmail.com</a>
 */

abstract class TestContext extends AkkaTestkitSpecs2Support {

}

class UserSessionTest extends Specification with NoTimeConversions {
  sequential

  val TestUsername = "testuser"

  "A UserSession" should {

    "send a LoginResult to the connector when it is created" in new TestContext {
      val connector = TestProbe()
      val askLogin = AskLogin(TestUsername, "pass")
      val userSession = system.actorOf(UserSession.props(askLogin, connector.ref))

      val expectedLoginResult = LoginResult(Ok, TestUsername)
      connector.expectMsg(1.second, expectedLoginResult)
    }

    "send a LoginResult to the connector when it is created and when " +
      "correct user reconnects with same connector" in new TestContext {
        val connector = TestProbe()
        val askLogin = AskLogin(TestUsername, "pass")
        val userSession = system.actorOf(UserSession.props(askLogin, connector.ref))
        val expectedLoginResult = LoginResult(Ok, TestUsername)
        connector.expectMsg(1.second, expectedLoginResult)

        userSession ! (askLogin, connector.ref)
        connector.expectMsg(1.second, expectedLoginResult)
      }

  }

}
