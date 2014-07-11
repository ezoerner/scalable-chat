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

package scalable.server

import akka.actor._
import akka.testkit._
import org.specs2.mutable._
import org.specs2.time.NoTimeConversions

import scala.concurrent.duration._

/**
 * A tiny class that can be used as a Specs2 'context'.
 * Thanks to Age Mooij for posting the example at
 * <a href="http://blog.xebia.com/2012/10/01/testing-akka-with-specs2/">http://blog.xebia.com/2012/10/01/testing-akka-with-specs2/</a>
 */
abstract class AkkaTestkitSpecs2Support extends TestKit(ActorSystem())
    with After
    with ImplicitSender {
  // make sure we shut down the actor system after all tests have run
  override def after = system.shutdown()
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