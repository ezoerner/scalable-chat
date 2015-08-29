package scalable.messaging.tcp

import akka.stream.stage.{Context, PushStage, SyncDirective}
import akka.util.ByteString
import org.scalactic.TypeCheckedTripleEquals._

/** Stage for extracting a ByteString message frame from a flow of bytes.
  * Each frame should start with a header consisting of an integer length
  * of the following payload.
  */
final class MessageStage extends PushStage[Byte, ByteString] {
  private val lengthBuilder = ByteString.newBuilder
  private val payloadBuilder = ByteString.newBuilder
  private var payloadLength: Int = _

  override def onPush(
    elem: Byte,
    ctx: Context[ByteString]
  ): SyncDirective = {

      def headerIsComplete = lengthBuilder.length === Integer.BYTES

      def extractPayloadLength(): Unit =
        payloadLength = lengthBuilder.result().iterator.getInt

      def addToPayload(): SyncDirective = {
        payloadBuilder.putByte(elem)
        if (payloadBuilder.length == payloadLength) {
          val bs = payloadBuilder.result()
          reset()
          ctx.push(bs)
        }
        else
          ctx.pull()
      }

      def addToHeader(): SyncDirective = {
        lengthBuilder.putByte(elem)
        if (lengthBuilder.length == 4 && {
          extractPayloadLength()
          payloadLength == 0
        })
          ctx.push(ByteString.empty)
        else
          ctx.pull()
      }

    if (headerIsComplete)
      addToPayload()
    else
      addToHeader()
  }

  private def reset() = {
    payloadLength = -1
    payloadBuilder.clear()
    lengthBuilder.clear()
  }
}
