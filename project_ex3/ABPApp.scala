package project_ex3

import akka.actor._
import scala.util.Random
import akka.event.Logging
import scala.collection.mutable.Queue
import scala.concurrent.duration._




case class Message(bit: Int, content: String)
case class Ack(bit: Int)
case object Start
case object TriggerNext

//  Trans
class Trans(receiver: ActorRef, senderRef: ActorRef, correct: Boolean, failure: Boolean) extends Actor {
  val log = Logging(context.system, this)
  import context.dispatcher          //this import is here because it only exists once an actor is created

  def receive: Receive = {
    case msg: Message =>
      if (failure) {
        log.info(s"[Trans] FALHA TOTAL: perdeu '${msg.content}' (bit=${msg.bit})")
        context.system.scheduler.scheduleOnce(
          500.millis,
          senderRef,
          TriggerNext
        )
      } else if (correct) {
        log.info(s"[Trans] Enviando: ${msg.content} (bit=${msg.bit})")
        receiver ! msg
      } else {
        if (Random.nextBoolean()) {
          log.info(s"[Trans] Enviando: ${msg.content} (bit=${msg.bit})")
          receiver ! msg
          while (Random.nextBoolean()) {
            log.info(s"[Trans] Duplicacao de mensagem: ${msg.content} (bit=${msg.bit})")
            receiver ! msg
          }
        } else {
          log.info(s"[Trans] PERDEU mensagem: ${msg.content}")
          senderRef ! TriggerNext
        }
      }
  }
}





//  AckChannel
class AckChannel(sender: ActorRef, receiver: ActorRef, correct: Boolean, failure: Boolean) extends Actor {
  val log = Logging(context.system, this)

  def receive: Receive = {
    case ack: Ack =>
      if (failure) {
        log.info(s"[AckChannel] FALHA TOTAL: perdeu Ack(${ack.bit})")
        //Nothing is sent, neither to sender nor receiver
      } else if (correct) {
        log.info(s"[AckChannel] Enviando Ack(${ack.bit})")
        sender ! ack
      } else {
        if (Random.nextBoolean()) {
          log.info(s"[AckChannel] Enviando Ack(${ack.bit})")
          sender ! ack
          while (Random.nextBoolean()) {
            log.info(s"[AckChannel] Duplicacao de Ack(${ack.bit})")
            sender ! ack
          }
        } else {
          log.info(s"[AckChannel] PERDEU Ack(${ack.bit})")
          receiver ! ack
        }
      }
  }
}


//  Receiver
class Receiver(getSender: () => ActorRef, correct: Boolean, failure: Boolean) extends Actor {
  val log = Logging (context.system, this)
  val ackChannel = context.actorOf(Props(new AckChannel(getSender(), this.self, correct, failure)), "ack-channel")
  var expectedBit = 0

  def receive: Receive = {
    case Message(bit, content) =>
      if (bit == expectedBit) {
        log.info(s"[Receiver] Entregou: '$content' com bit $bit")
        expectedBit = 1 - expectedBit
        ackChannel ! Ack(bit)
      } else {
        log.info(s"[Receiver] Ignorou duplicado com bit $bit")
      }
      
    case Ack(bit) =>
      log.info(s"[Receiver] Reenvia ack: com bit $bit")
      ackChannel ! Ack(bit)
  }
}

//  Sender
class Sender(receiver: ActorRef, correct: Boolean, failure: Boolean,var messages: Queue[String]) extends Actor {
  val log = Logging (context.system, this)
  val trans = context.actorOf(Props(new Trans(receiver, this.self, correct, failure)), "trans")
  var bit = 0
  

  def receive: Receive = {
    case Start =>
      self ! TriggerNext

    case TriggerNext =>
      if (messages.nonEmpty) {
        val msg = messages.head
        log.info(s"[Sender] A enviar '$msg' com bit $bit")
        trans ! Message(bit, msg)
      }

    case Ack(receivedBit) =>
      if (receivedBit == bit) {
        log.info(s"[Sender] Recebeu Ack correto: $receivedBit")
        bit = 1 - bit
      if (messages.nonEmpty) {
        messages = messages.tail
      }

      if (messages.isEmpty) {
        log.info("[Sender] Todas as mensagens enviadas com sucesso. A terminar o sistema.")
        context.system.terminate()
      } else {
        self ! TriggerNext
      }
    } else {
      log.info(s"[Sender] Ignorou Ack errado: $receivedBit (esperava $bit)")
    }
  }
}

//  App
object ABPApp extends App {
  lazy val system = ActorSystem("ABPSystem")

  val correct = false
  val failure= true
  val messages: Queue[String] = Queue("msg1", "msg2", "msg3") // queue with example messages

  var senders: ActorRef = null

  val receiver = system.actorOf(Props(new Receiver(() => senders, correct, failure)), "receiver")

  senders = system.actorOf(Props(new Sender(receiver, correct, failure, messages)), "sender")

  senders ! Start

  Thread.sleep(5000)

}

/*
A hieraquia que existe:

         "ABPSystem"
        /           \
    "Sender"      "Receiver"
        |              |
    "Trans"        "Ack_channel"
*/