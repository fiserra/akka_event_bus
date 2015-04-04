import akka.event.ActorEventBus
import akka.event.LookupClassification
import akka.actor.ActorSystem
import akka.actor.Props
import akka.actor.Actor

case class ZombieSighting(zombieTag: String, lat: Double, long: Double, alt: Double)

case class ZombieSightingEvent(topic: String, sighting: ZombieSighting)

class ZombieSightingLookupEventBus extends ActorEventBus with LookupClassification {
  type Event = ZombieSightingEvent
  type Classifier = String

  protected def mapSize(): Int = 10

  protected def classify(event: Event): Classifier = {
    event.topic
  }

  protected def publish(event: Event, subscriber: Subscriber): Unit = {
    subscriber ! event.sighting
  }
}

object ZombieTrackerApp extends App {

  val system = ActorSystem()
  val eventBus = new ZombieSightingLookupEventBus

  val subscriber = system.actorOf(Props(new Actor {
    def receive = {
      case s: ZombieSighting => println(s"Spotted a zombie! $s")
    }
  }))

  val indifferentSubscriber = system.actorOf(Props(new Actor {
    def receive = {
      case s: ZombieSighting => println(s"I saw a zombie, but I don't give a crap.")
    }
  }))

  eventBus.subscribe(subscriber, "/zombies")
  eventBus.subscribe(indifferentSubscriber, "/zombies")

  eventBus.publish(ZombieSightingEvent("/zombies", ZombieSighting("FATZOMBIE1", 37.1234, 45.1234, 100.0)))
  eventBus.publish(ZombieSightingEvent("/zombies", ZombieSighting("SKINNYBOY", 30.1234, 50.1234, 12.0)))

  // And this one will go off into deadletter - nobody is subscribed to this.
  eventBus.publish(ZombieSightingEvent("/zombies/foo/bar/baz", ZombieSighting("OTHERONE", 35.0, 42.5, 50.0)))
  system.shutdown()
}