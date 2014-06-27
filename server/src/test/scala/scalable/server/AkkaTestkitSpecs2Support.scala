package scalable.server

import akka.actor._
import akka.testkit._
import org.specs2.mutable._
import org.specs2.time.NoTimeConversions

import scala.concurrent.duration._
import scalable.GlobalEnv

/**
 * A tiny class that can be used as a Specs2 'context'.
 * Thanks to Age Mooij for posting the example at
 * <a href="http://blog.xebia.com/2012/10/01/testing-akka-with-specs2/">http://blog.xebia.com/2012/10/01/testing-akka-with-specs2/</a>
 */
abstract class AkkaTestkitSpecs2Support extends TestKit(GlobalEnv.createActorSystem("default"))
                                                with After
                                                with ImplicitSender {
  // make sure we shut down the actor system after all tests have run
  override def after = GlobalEnv.shutdownActorSystem()
}

/* Both Akka and Specs2 add implicit conversions for adding time-related
   methods to Int. Mix in the Specs2 NoTimeConversions trait to avoid a clash. */
class ExampleSpec extends Specification with NoTimeConversions {
  sequential // forces all tests to be run sequentially (optional)

  "A TestKit" should {
    /* for every case where you would normally use "in", use 
       "in new AkkaTestkitSpecs2Support" to create a new 'context'. */
    "work properly with Specs2 unit tests" in new AkkaTestkitSpecs2Support {
      within(1.second) {
        system.actorOf(Props(new Actor {
          def receive = { case x ⇒ sender ! x }
        })) ! "hallo"

        expectMsgType[String] must be equalTo "hallo"
      }
    }
  }
}