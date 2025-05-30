error id: file://<WORKSPACE>/server/src/main/scala/cp/serverPr/ServerState.scala:local2
file://<WORKSPACE>/server/src/main/scala/cp/serverPr/ServerState.scala
empty definition using pc, found symbol in pc: 
found definition using semanticdb; symbol local2
empty definition using fallback
non-local guesses:

offset: 2002
uri: file://<WORKSPACE>/server/src/main/scala/cp/serverPr/ServerState.scala
text:
```scala
package cp.serverPr

import java.util.concurrent.Semaphore
import scala.collection.mutable
import scala.concurrent.{Future, Promise}
import scala.sys.process._
import scala.util.Try
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.blocking

case class CommandRequest(id: Int, cmd: String, userIp: String, promise: Promise[String])

class ServerState(maxConcurrent: Int) {
  @volatile private var counter: Int = 0
  private def nextId(): Int = this.synchronized {
    counter += 1
    counter
  }

  private val sem = new Semaphore(maxConcurrent)
  private val queue = new mutable.Queue[CommandRequest]()
  private val resultQueue = new mutable.Queue[CommandRequest]()

  // Thread que executa comandos concorrentes
  private val executorThread = new Thread(() => {
    while (true) {
      val requestOpt = this.synchronized {
        if (queue.nonEmpty) Some(queue.dequeue()) else None
      }

      requestOpt.foreach { req =>
        Future {
          blocking { sem.acquire() }
          val output = new StringBuilder
          val logger = ProcessLogger(line => { output.append(line + "\n"); () })

          val shellCommand =
            if (System.getProperty("os.name").toLowerCase.contains("win")) {
              Seq("wsl", "-e", "bash", "-c", req.cmd)
            } else {
              Seq("sh", "-c", req.cmd)
            }

          val result = Try {
            shellCommand.!(logger)
            val timestamp = java.time.LocalDateTime.now()
            s"[${req.id}] Result from running '${req.cmd}' for user ${req.userIp}:\n${output.toString}\nFinished at: $timestamp"
            }.recover {
              case e: Exception =>
                val timestamp = java.time.LocalDateTime.now()
                s"[${req.id}] Error running '${req.cmd}': ${e.getMessage}\nFinished at: $timestamp"
            }.get


          req.promise.success(result)
          sem.release()
        }
      }

      if (requestOpt.isEmpty) blocking { Thread.sleep(@@5) }
    }
  })

  executorThread.setDaemon(true)
  executorThread.start()

  def submitCommand(cmd: String, userIp: String): Future[String] = {
    val promise = Promise[String]()
    val id = nextId()
    val req = CommandRequest(id, cmd, userIp, promise)

    this.synchronized {
      queue.enqueue(req)
      resultQueue.enqueue(req)
    }

    // Polling sem blocking: espera até ser o topo e estar completo
    def waitForTurn(): Future[String] = {
      Future {
        var done = false
        var result = ""
        while (!done) {
          blocking {
            this.synchronized {
              if (resultQueue.headOption.contains(req) && promise.isCompleted) {
                result = promise.future.value.get.get
                resultQueue.dequeue()
                done = true
              }
            }
          }
          if (!done) Thread.sleep(5) // pequeno delay para evitar busy loop (Solução 1)
        }
        result
      }
    }

    waitForTurn()
  }

  def toHtml: String = s"""
<p><strong>counter:</strong> $counter</p>
<p><strong>queued:</strong> ${queue.size}</p>
<p><strong>waiting:</strong> ${resultQueue.size}</p>
<p><strong>running:</strong> ${maxConcurrent - sem.availablePermits()}</p>
""".stripMargin
}

```


#### Short summary: 

empty definition using pc, found symbol in pc: 