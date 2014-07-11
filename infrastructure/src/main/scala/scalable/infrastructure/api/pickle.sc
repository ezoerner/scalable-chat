import scala.pickling._
import binary._
import scalable.infrastructure.api.{Joined, SerializableMessage}

val lst1 = List(1, 2, 3, 4)
val pckl = lst1.pickle
val bytes: Array[Byte] = pckl.value
val lst2 = bytes.unpickle[List[Int]]
lst1 == lst2
val joined: SerializableMessage = Joined("username", "roomName")
val bytes2 = joined.toByteString
val newJoined = SerializableMessage(bytes2)
joined == newJoined
