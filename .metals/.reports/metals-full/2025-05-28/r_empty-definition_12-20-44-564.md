error id: file://<WORKSPACE>/server/src/main/scala/cp/serverPr/ServerState.scala:local1
file://<WORKSPACE>/server/src/main/scala/cp/serverPr/ServerState.scala
empty definition using pc, found symbol in pc: 
empty definition using semanticdb
empty definition using fallback
non-local guesses:
	 -scala/sys/process/acquired.
	 -scala/sys/process/acquired#
	 -scala/sys/process/acquired().
	 -acquired.
	 -acquired#
	 -acquired().
	 -scala/Predef.acquired.
	 -scala/Predef.acquired#
	 -scala/Predef.acquired().
offset: 830
uri: file://<WORKSPACE>/server/src/main/scala/cp/serverPr/ServerState.scala
text:
```scala
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
    val current = concurrent.get()
    if (current < maxConcurrent) {
      acqui@@red = concurrent.compareAndSet(current, current + 1)
    } else {
      while (!acquired) {
        queued.incrementAndGet()
        Thread.sleep(10)
        queued.decrementAndGet()
        acquired = concurrent.compareAndSet(current, current + 1)
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
<p><strong>counter:</strong> $counter</p>
<p><strong>queued: </strong> ${queued.get()}</p>
<p><strong>completed: </strong> ${completed.get()}</p>
<p><strong>running:</strong> ${concurrent.get()}</p>
""".stripMargin
}
```


#### Short summary: 

empty definition using pc, found symbol in pc: 