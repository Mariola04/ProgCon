package cp.serverPr

import java.util.concurrent.Semaphore
import scala.sys.process._
import java.util.concurrent.atomic.AtomicInteger

class ServerState(maxConcurrent: Int) {
  private val semaphore = new Semaphore(maxConcurrent)
  private val queued = new AtomicInteger(0)
  private val completed = new AtomicInteger(0)

  @volatile var counter: Int = 0

  private def nextId(): Int = this.synchronized {
    counter += 1
    counter
  }

  def runProcess(cmd: String, userIp: String): String = {
    queued.incrementAndGet()
    semaphore.acquire()
    queued.decrementAndGet()

    val id = nextId()
    try {
      val output = new StringBuilder
      val logger = ProcessLogger(line => { output.append(line + "\n"); () })
      Seq("sh", "-c", cmd).!(logger)
      completed.incrementAndGet()
      s"[$id] Result from running '$cmd' for user $userIp:\n${output.toString}"
    } catch {
      case e: Exception => s"[$id] Error running '$cmd': ${e.getMessage}"
    } finally {
      semaphore.release()
    }
  }


//TODO: IMPLEMENTAR COUNTER DE DEQUEUE (DEBUG!!!) 
  def toHtml: String =
    s"""
      |<p><strong>counter:</strong> $counter</p>
      |<p><strong>queued:</strong> ${queued.get()}</p>
      |<p><strong>completed:</strong> ${completed.get()}</p>
      |""".stripMargin
}
