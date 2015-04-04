import akka.actor.{ActorSystem, Props, Actor}
import akka.event.{SubchannelClassification, ActorEventBus}
import akka.util.Subclassification

class ZombieSightingSubclassEventBus extends ActorEventBus with SubchannelClassification {
  type Event = ZombieSightingEvent
  type Classifier = String

  protected def classify(event: Event): Classifier = event.topic

  protected def subclassification = new Subclassification[Classifier] {
    def isEqual(x: Classifier, y: Classifier) = x == y

    def isSubclass(x: Classifier, y: Classifier) = x.startsWith(y)
  }

  protected def publish(event: Event, subscriber: Subscriber): Unit = {
    subscriber ! event.sighting
  }
}

object ZombieTrackerSubApp extends App {

  val system = ActorSystem()

  val eventBus = new ZombieSightingSubclassEventBus

  val subscriber = system.actorOf(Props(new Actor {
    def receive = {
      case s: ZombieSighting => println(s"Spotted a zombie! $s")
    }
  }))

  val westCoastSightingHandler = system.actorOf(Props(new Actor {
    def receive = {
      case s: ZombieSighting => println(s"West coast zombie $s!!")
    }
  }))

  val eastCoastSightingHandler = system.actorOf(Props(new Actor {
    def receive = {
      case s: ZombieSighting => println(s"East coast zombie $s!!")
    }
  }))

  eventBus.subscribe(subscriber, "/zombies")
  eventBus.subscribe(westCoastSightingHandler, "/zombies/WEST")
  eventBus.subscribe(eastCoastSightingHandler, "/zombies/EAST")

  eventBus.publish(ZombieSightingEvent("/zombies/WEST", ZombieSighting("FATZOMBIE1", 37.1234, 45.1234, 100.0)))
  eventBus.publish(ZombieSightingEvent("/zombies/EAST", ZombieSighting("SKINNYBOY", 30.1234, 50.1234, 12.0)))

  // And this one will NOT go off into deadletter like before ... this satisfies "startsWith" on the first subscriber
  eventBus.publish(ZombieSightingEvent("/zombies/foo/bar/baz", ZombieSighting("OTHERONE", 35.0, 42.5, 50.0)))
  system.shutdown
}