package cp.serverPr

// TODO: fazer versao com atomic counter sem sync block ou com e mais sofisticado
import java.util.concurrent.atomic.AtomicInteger
import scala.sys.process._

class ServerState(maxConcurrent: Int) {
  // === Synchronized + volatile ===
  @volatile private var counter: Int = 0

  // === Lock-free shared state ===
  private val concurrent = new AtomicInteger(0)
  private val queued = new AtomicInteger(0)
  private val completed = new AtomicInteger(0)

  // === Volatile variable ===
  @volatile var lastCommand: String = ""

  private def nextId(): Int = this.synchronized {
    counter += 1
    counter
  }


  def runProcess(cmd: String, userIp: String): String = {
    // Try to acquire a slot without blocking (lock-free)
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

    val id = nextId()
    println(s"START $cmd")
    try {
      val output = new StringBuilder
      val logger = ProcessLogger(line => { output.append(line + "\n"); () })

      val isWindows = System.getProperty("os.name").toLowerCase.contains("win")
      val shellCommand =
        if (isWindows) {
          // using WSL if available
          Seq("wsl", "-e", "bash", "-c", cmd)
        } else {
          // normal Linux
          Seq("sh", "-c", cmd)
        }
      shellCommand.!(logger)

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
       |<p><strong>counter:</strong> $counter</p>
       |<p><strong>queued: </strong> ${queued.get()}</p>
       |<p><strong>completed: </strong> ${completed.get()}</p>
       |<p><strong>running:</strong> ${concurrent.get()}</p>
       |""".stripMargin
}