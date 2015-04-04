package chatbus

import akka.event.{SubchannelClassification, ActorEventBus}
import akka.util.Subclassification

object ChatSegments {
  val Player = "players"
  val Sector = "sectors"
  val Group = "groups"
}

case class ChatCoordinate(segment: String, target: Option[String])
sealed trait Chat
case class ChatMessage(source: String, message: String) extends Chat
case class ChatEvent(coord: ChatCoordinate, msg: ChatMessage) extends Chat

class ChatEventBus extends ActorEventBus with SubchannelClassification {
  override type Classifier = ChatCoordinate
  override type Event = ChatEvent

  override protected implicit def subclassification: Subclassification[Classifier] =
    new Subclassification[Classifier] {
      override def isEqual(x: Classifier, y: Classifier): Boolean =
        x.segment == y.segment && y.target == x.target

      override def isSubclass(x: Classifier, y: Classifier): Boolean =
        x.segment == y.segment && x.target == None

    }

  override protected def classify(event: Event): Classifier = event.coord

  override protected def publish(event: Event, subscriber: Subscriber): Unit =
    subscriber ! event.msg
}