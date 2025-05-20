package cp.serverPr

import java.util.concurrent.atomic.AtomicInteger
import scala.sys.process._

class ServerState(maxConcurrent: Int) {
  // Lock-free shared state using AtomicInteger
  private val concurrent = new AtomicInteger(0)
  private val queued = new AtomicInteger(0)
  private val completed = new AtomicInteger(0)
  private val counter = new AtomicInteger(0)

  /** Execute the command and return its result. */
  def runProcess(cmd: String, userIp: String): String = {
    // Try to acquire a slot without blocking (is this lock free?)
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

    val id = counter.incrementAndGet()
    try {
      val output = new StringBuilder
      val logger = ProcessLogger(line => { output.append(line + "\n"); () })
      Seq("sh", "-c", cmd).!(logger)
      completed.incrementAndGet()
      s"[$id] Result from running '$cmd' for user $userIp:\n${output.toString}"
    } catch {
      case e: Exception => s"[$id] Error running '$cmd': ${e.getMessage}"
    } finally {
      concurrent.decrementAndGet(); ()
    }
  }

  def toHtml: String =
    s"""
      |<p><strong>counter:</strong> ${counter.get()}</p>
      |<p><strong>queued:</strong> ${queued.get()}</p>
      |<p><strong>completed:</strong> ${completed.get()}</p>
      |""".stripMargin
}