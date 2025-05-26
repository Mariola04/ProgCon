package project_ex3

import akka.actor._
import scala.util.Random
import akka.event.Logging
import scala.collection.mutable.Queue

case class Message(bit: Int, content: String)
case class Ack(bit: Int)
case object Start
case object TriggerNext

//  Trans
class Trans(receiver: ActorRef, sender: ActorRef, correct: Boolean, failiure: Boolean) extends Actor {
  val log = Logging(context.system, this)

  def receive: Receive = {
    case msg: Message =>
      if (failiure) {
        log.info(s"[Trans] FALHA TOTAL: perdeu '${msg.content}' (bit=${msg.bit})")
        // Nada é enviado, nem ao receiver nem ao sender
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
          sender ! TriggerNext
        }
      }
  }
}


//  AckChannel
class AckChannel(sender: ActorRef, receiver: ActorRef, correct: Boolean, failiure: Boolean) extends Actor {
  val log = Logging(context.system, this)

  def receive: Receive = {
    case ack: Ack =>
      if (failiure) {
        log.info(s"[AckChannel] FALHA TOTAL: perdeu Ack(${ack.bit})")
        // Nada é enviado, nem ao sender nem ao receiver
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
class Receiver(getSender: () => ActorRef, correct: Boolean, failiure: Boolean) extends Actor {
  val log = Logging (context.system, this)
  val ackChannel = context.actorOf(Props(new AckChannel(getSender(), this.self, correct, failiure)), "ack-channel")
  var expectedBit = 0

  def receive: Receive = {
    case Message(bit, content) =>
      if (bit == expectedBit) {
        log.info(s"[Receiver] Entregou: '$content' com bit $bit")
        expectedBit = 1 - expectedBit
        ackChannel ! Ack(bit) // assim esta bem
      } else {
        log.info(s"[Receiver] Ignorou duplicado com bit $bit")
      }
      //ackChannel ! Ack(bit) // TODO: acho que isto n pode ficar aqui.....se ignore manda na mesma..... DONE
    case Ack(bit) =>
      log.info(s"[Receiver] Reenvia ack: com bit $bit")
      ackChannel ! Ack(bit)
  }
}

//  Sender
class Sender(receiver: ActorRef, correct: Boolean, failiure: Boolean,var messages: Queue[String]) extends Actor {
  val log = Logging (context.system, this)
  val trans = context.actorOf(Props(new Trans(receiver, this.self, correct, failiure)), "trans")
  var bit = 0
  //var messages = List("msg1", "msg2", "msg3")

  def receive: Receive = {
    case Start =>
      self ! TriggerNext

    case TriggerNext =>
      if (messages.nonEmpty) {
        val msg = messages.head
        log.info(s"[Sender] A enviar '$msg' com bit $bit")
        trans ! Message(bit, msg)
      }

    case Ack(receivedBit) => // TODO: melhorar a terminação, acho que n está muioto bem.....
      if (messages.isEmpty) context.system.terminate()

      if (receivedBit == bit) {
        log.info(s"[Sender] Recebeu Ack correto: $receivedBit")
        bit = 1 - bit
        messages = messages.tail
        if (messages.isEmpty) context.system.terminate()
        else self ! TriggerNext
      } else {
        log.info(s"[Sender] Ignorou Ack errado: $receivedBit")
      }
  }
}

//  App
object ABPApp extends App {
  lazy val system = ActorSystem("ABPSystem")

  val correct = false // true para tudo certo, ex2 pede isso
  val failiure= true // TODO: o failiure de momemnto n faz nada ,logo tem que estar a falso
  val messages: Queue[String] = Queue("msg1", "msg2", "msg3") // queue com as menssagens

  var senders: ActorRef = null

  val receiver = system.actorOf(Props(new Receiver(() => senders, correct, failiure)), "receiver")

  senders = system.actorOf(Props(new Sender(receiver, correct, failiure, messages)), "sender")

  senders ! Start

}

/*
A hieraquia que existe:

         "ABPSystem"
        /           \
    "Sender"      "Receiver"
        |              |
    "Trans"        "Ack_channel"
*/