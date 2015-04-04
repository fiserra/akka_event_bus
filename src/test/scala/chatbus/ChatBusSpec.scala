package chatbus

import akka.actor.{ActorSystem, Props, Actor}
import akka.testkit.{TestProbe, TestKit, ImplicitSender}
import chatbus.ChatSegments.{Sector, Player}
import org.scalatest.WordSpecLike
import org.scalatest.Matchers
import org.scalatest.BeforeAndAfterAll

object ChatBusSpec {
  val toKevin = ChatEvent(ChatCoordinate(Player, Some("Kevin")), ChatMessage("system", "This goes just to Kevin"))
  val toBob = ChatEvent(ChatCoordinate(Player, Some("Bob")), ChatMessage("system", "This goes just to Bob"))
  val toAllPlayers = ChatEvent(ChatCoordinate(Player, None), ChatMessage("kevin", "This goes to all players"))

  val toSectorAlpha = ChatEvent(ChatCoordinate(Sector, Some("Alpha")), ChatMessage("system", "Sector Alpha is about to explode."))
  val toSectorBeta = ChatEvent(ChatCoordinate(Sector, Some("Beta")), ChatMessage("system", "Sector Beta is about to explode."))
}

class ChatBusSpec(_system: ActorSystem)
  extends TestKit(_system)
  with ImplicitSender
  with WordSpecLike
  with Matchers
  with BeforeAndAfterAll {

  import ChatBusSpec._

  val theProbe = TestProbe()

  def this() = this(ActorSystem("ChatBusSpec"))

  def getEchoSubscriber = {
    system.actorOf(Props(new Actor {
      def receive = {
        case m: ChatMessage => theProbe.ref ! m
      }
    }))
  }

  override def afterAll() {
    TestKit.shutdownActorSystem(system)
  }

  "A chat event bus" must {
    "send a private message to a player with no snoopers" in {
      val eventBus = new ChatEventBus
      val kevin = getEchoSubscriber
      val bob = getEchoSubscriber
      eventBus.subscribe(kevin, ChatCoordinate(Player, Some("Kevin")))
      eventBus.subscribe(bob, ChatCoordinate(Player, Some("Bob")))
      eventBus.publish(toKevin)
      theProbe.expectMsg(toKevin.msg)
      theProbe.expectNoMsg()
    }
    "send a single message to all players" in {
      val eventBus = new ChatEventBus
      val kevin = getEchoSubscriber
      val bob = getEchoSubscriber
      eventBus.subscribe(kevin, ChatCoordinate(Player, Some("Kevin")))
      eventBus.subscribe(bob, ChatCoordinate(Player, Some("Bob")))
      eventBus.publish(toAllPlayers)
      // Each player should receive one of these, so the probe should bounce it back twice.
      theProbe.expectMsg(toAllPlayers.msg)
      theProbe.expectMsg(toAllPlayers.msg)
    }

    "send to all players in a sector should only deliver once per player" in {
      val eventBus = new ChatEventBus
      val kevin = getEchoSubscriber

      eventBus.subscribe(kevin, ChatCoordinate(Player, Some("Kevin")))
      eventBus.subscribe(kevin, ChatCoordinate(Sector, Some("Alpha")))
      eventBus.publish(toSectorAlpha)
      theProbe.expectMsg(toSectorAlpha.msg)
      theProbe.expectNoMsg()
    }

    "support a player moving from one sector to another" in {
      val eventBus = new ChatEventBus
      val kevin = getEchoSubscriber

      eventBus.subscribe(kevin, ChatCoordinate(Player, Some("Kevin")))
      eventBus.subscribe(kevin, ChatCoordinate(Sector, Some("Alpha")))
      eventBus.publish(toKevin)
      theProbe.expectMsg(toKevin.msg)
      eventBus.publish(toSectorAlpha)
      theProbe.expectMsg(toSectorAlpha.msg)
      eventBus.unsubscribe(kevin, ChatCoordinate(Sector, Some("Alpha")))
      eventBus.subscribe(kevin, ChatCoordinate(Sector, Some("Beta")))
      eventBus.publish(toSectorBeta)
      theProbe.expectMsg(toSectorBeta.msg)
    }
  }
}