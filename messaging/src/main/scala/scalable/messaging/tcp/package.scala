package scalable.messaging

import java.nio.ByteOrder

package object tcp {
  implicit val byteOrder = ByteOrder.BIG_ENDIAN
}
