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

  // Thread that executes concurrent commands
  private val executorThread = new Thread(() => {
    while (true) {
      val requestOpt = this.synchronized {
        if (queue.nonEmpty) Some(queue.dequeue()) else None
      }

      requestOpt.foreach { req =>
        println(s"[${req.id}] Executing command: ${req.cmd}") // print for bad server
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

          println(s"[${req.id}] Completed command, result ready.") // print for bad server
          req.promise.success(result)
          sem.release()
        }
      }

      if (requestOpt.isEmpty) blocking { Thread.sleep(5) }
    }
  })

  executorThread.setDaemon(true)
  executorThread.start()

  def submitCommand(cmd: String, userIp: String): Future[String] = {
    val promise = Promise[String]()
    val id = nextId()
    val req = CommandRequest(id, cmd, userIp, promise)

    this.synchronized { // comment this line for bad server
      queue.enqueue(req)
      resultQueue.enqueue(req)
    } // comment this line for bad server
    println(s"[$id] Submitting command '$cmd' from $userIp") // print for bad server


    // Polling without blocking: wiats til being head and completes
    def waitForTurn(): Future[String] = {
      Future {
        var done = false
        var result = ""
        while (!done) {
          blocking {
            this.synchronized {
              println(s"head ${resultQueue.headOption} res: $req") // print for bad server
              if (resultQueue.headOption.contains(req) && promise.isCompleted) {
                result = promise.future.value.get.get
                resultQueue.dequeue()
                done = true
              }
            }
          }
          if (!done) Thread.sleep(5) // small delay to avoid busy loop
        }
        result
      }
    }

    waitForTurn()
  }

  def toHtml: String = s"""
       |<p><strong>counter:</strong> $counter</p>
       |<p><strong>queued:</strong> ${queue.size}</p>
       |<p><strong>waiting:</strong> ${resultQueue.size}</p>
       |<p><strong>running:</strong> ${maxConcurrent - sem.availablePermits()}</p>
       |""".stripMargin
}