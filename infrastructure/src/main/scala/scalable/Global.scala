package scalable

import akka.actor.ActorSystem

/**
 * // TODO: Add class description here.
 *
 * @author Eric Zoerner <a href="mailto:eric.zoerner@gmail.com">eric.zoerner@gmail.com</a>
 */

object Global {

  private[scalable] var _defaultSystem: ActorSystem = _

  implicit def system: ActorSystem = _defaultSystem

}
