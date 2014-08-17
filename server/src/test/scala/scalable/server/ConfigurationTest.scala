package scalable.server

import org.specs2.mutable.Specification

/**
 * Tests for Configuration.
 *
 * @author Eric Zoerner <a href="mailto:eric.zoerner@gmail.com">eric.zoerner@gmail.com</a>
 */
class ConfigurationTest extends Specification {

  "Configuration" should {
    "have expected service role" in {
      Configuration.serviceRole === "service"
    }
  }
}
