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

package scalable.messaging.tcp

import akka.actor.ActorSystem
import akka.util.{ ByteString, ByteStringBuilder }

import scala.annotation.tailrec
import scala.collection.mutable.ListBuffer
import scalable.messaging.api.SerializableMessage

/** @author Eric Zoerner <a href="mailto:eric.zoerner@gmail.com">eric.zoerner@gmail.com</a>
  */
class SimpleBuffer {

  implicit val byteOrder = java.nio.ByteOrder.BIG_ENDIAN

  private var buffer: ByteString = ByteString()
  private var _expectedByteCount: Int = 0

  def nextMessageBytes(incomingBytes: ByteString): List[ByteString] = {
    val resultBuffer = new ListBuffer[ByteString]()

      def isBufferReady = expectedByteCount() > 0 && bufferHasCompleteFrame

      def expectedByteCount() = {
        if (_expectedByteCount > 0) {
          _expectedByteCount
        }
        else if (buffer.size >= 4) {
          val iterator = buffer.iterator
          buffer = buffer.drop(4)
          _expectedByteCount = iterator.getInt
          _expectedByteCount
        }
        else 0
      }

      def bufferHasCompleteFrame = {
        val count = expectedByteCount()
        count <= buffer.size
      }

      def takeFrame() = {
        val (prefix, suffix) = buffer.splitAt(expectedByteCount())
        resultBuffer += prefix
        buffer = suffix
        _expectedByteCount = 0
      }

      @tailrec
      def process(): Unit = {
        if (isBufferReady) {
          takeFrame()
          process()
        }
      }

    buffer = buffer ++ incomingBytes
    process()
    resultBuffer.toList
  }

  def serializableMessageWithLength(msg: SerializableMessage)(implicit system: ActorSystem): ByteString = {
    val dataBytes: Array[Byte] = msg.toByteArray
    val builder = new ByteStringBuilder
    builder.putInt(dataBytes.size)
    builder.putBytes(dataBytes)
    builder.result()
  }
}
