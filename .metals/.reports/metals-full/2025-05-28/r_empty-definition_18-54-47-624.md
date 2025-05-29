error id: file://<WORKSPACE>/server/src/main/scala/cp/serverPr/ServerState.scala:java/util/concurrent/Semaphore#
file://<WORKSPACE>/server/src/main/scala/cp/serverPr/ServerState.scala
empty definition using pc, found symbol in pc: java/util/concurrent/Semaphore#
empty definition using semanticdb
empty definition using fallback
non-local guesses:
	 -java/util/concurrent/Semaphore#
	 -scala/sys/process/Semaphore#
	 -Semaphore#
	 -scala/Predef.Semaphore#
offset: 1126
uri: file://<WORKSPACE>/server/src/main/scala/cp/serverPr/ServerState.scala
text:
```scala
package cp.serverPr

import java.util.concurrent.atomic.{AtomicInteger}
import java.util.concurrent.Semaphore
import scala.sys.process._
import scala.concurrent.{Future, Promise}
import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global

class ServerState(maxConcurrent: Int) {

  // === Contadores ===
  @volatile private var counter: Int = 0
  private val concurrent = new AtomicInteger(0)
  private val queued = new AtomicInteger(0)
  private val completed = new AtomicInteger(0)
  @volatile var lastCommand: String = ""


  private def nextId(): Int = this.synchronized {
    counter += 1
    counter
  }

  // === Estruturas auxiliares ===
  private case class CommandEntry(id: Int, promise: Promise[String])
  private case class CommandRequest(cmd: String, userIp: String, promise: Promise[String])

  private val inputQueue = new mutable.Queue[CommandRequest]()
  private val outputQueue = new mutable.Queue[CommandEntry]()
  private val inputLock = new Object
  private val outputLock = new Object

  // === Semáforo para entrada sincronizada ===
  private val inputSemaphore = new Sema@@phore(1, true) // justo, FIFO

  // === Processador da fila de entrada ===
  private val inputProcessor: Thread = new Thread(() => {
    while (true) {
      val request: CommandRequest = inputLock.synchronized {
        while (inputQueue.isEmpty) inputLock.wait()
        inputQueue.dequeue()
      }
      processCommand(request)
    }
  })
  inputProcessor.setDaemon(true)
  inputProcessor.start()

  // === Entrada de comandos (invocado por Routes) ===
  def enqueueCommand(cmd: String, userIp: String): Future[String] = {
    val promise = Promise[String]()

    // Bloqueia a entrada até que seja enfileirado
    inputSemaphore.acquire()

    val request = CommandRequest(cmd, userIp, promise)

    inputLock.synchronized {
      inputQueue.enqueue(request)
      inputLock.notify()
    }

    inputSemaphore.release()

    promise.future
  }

  // === Processar um pedido (assíncrono) ===
  private def processCommand(req: CommandRequest): Unit = {
    val id = nextId()
    val promise = req.promise
    val entry = CommandEntry(id, promise)

    outputLock.synchronized {
      outputQueue.enqueue(entry)
    }

    Future {
      var acquired = false
      while (!acquired) {
        val current = concurrent.get()
        if (current < maxConcurrent) {
          acquired = concurrent.compareAndSet(current, current + 1)
        } else {
          queued.incrementAndGet()
          Thread.sleep(10)
          queued.decrementAndGet()
        }
      }

      println(s"START ${req.cmd}")
      val output = new StringBuilder
      val logger = ProcessLogger(line => { output.append(line + "\n"); ()})

      val isWindows = System.getProperty("os.name").toLowerCase.contains("win")
      val shellCommand = if (isWindows) Seq("wsl", "-e", "bash", "-c", req.cmd) else Seq("sh", "-c", req.cmd)

      try {
        shellCommand.!(logger)
        completed.incrementAndGet()
        val result = s"[$id] Result from running '${req.cmd}' for user ${req.userIp}:\n${output.toString}"
        promise.success(result)
      } catch {
        case e: Exception =>
          promise.success(s"[$id] Error running '${req.cmd}': ${e.getMessage}")
      } finally {
        concurrent.decrementAndGet()
        tryReleaseOutputs()
      }
    }; ()
  }

  // === Libertar outputs pela ordem da fila ===
  private def tryReleaseOutputs(): Unit = outputLock.synchronized {
    while (outputQueue.headOption.exists(_.promise.isCompleted)) {
      val entry = outputQueue.dequeue()
      println(entry.promise.future.value.get.get) // Simula envio ao cliente
    }
  }

  def toHtml: String =
    s"""
<p><strong>counter:</strong> $counter</p>
<p><strong>queued: </strong> ${queued.get()}</p>
<p><strong>completed: </strong> ${completed.get()}</p>
<p><strong>running:</strong> ${concurrent.get()}</p>
""".stripMargin
}

```


#### Short summary: 

empty definition using pc, found symbol in pc: java/util/concurrent/Semaphore#