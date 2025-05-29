error id: file://<WORKSPACE>/server/src/main/scala/cp/serverPr/ServerState.scala:java/util/concurrent/atomic/AtomicInteger#
file://<WORKSPACE>/server/src/main/scala/cp/serverPr/ServerState.scala
empty definition using pc, found symbol in pc: java/util/concurrent/atomic/AtomicInteger#
empty definition using semanticdb
empty definition using fallback
non-local guesses:
	 -java/util/concurrent/atomic/AtomicInteger#
	 -AtomicInteger#
	 -scala/Predef.AtomicInteger#
offset: 260
uri: file://<WORKSPACE>/server/src/main/scala/cp/serverPr/ServerState.scala
text:
```scala
package cp.serverPr

import scala.collection.mutable
import java.util.concurrent.atomic.AtomicInteger

object ServerState {
  private val MAX_CONCURRENT = 3
  private val queue = new mutable.Queue[(String, String => Unit)]()
  private val currentRunning = new @@AtomicInteger(0)

  @volatile var totalProcessed: Int = 0

  def runCommand(command: String, callback: String => Unit): Unit = synchronized {
    if (currentRunning.get() < MAX_CONCURRENT) {
      currentRunning.incrementAndGet()
      execute(command, callback)
    } else {
      queue.enqueue((command, callback))
    }
  }

  private def execute(command: String, callback: String => Unit): Unit = {
    val process = new Thread(() => {
      val output = try {
        val pb = new ProcessBuilder("bash", "-c", command)
        val proc = pb.start()
        val result = scala.io.Source.fromInputStream(proc.getInputStream).mkString
        proc.waitFor()
        result
      } catch {
        case e: Exception => s"Error: ${e.getMessage}"
      }

      callback(output)

      synchronized {
        totalProcessed += 1
        currentRunning.decrementAndGet()
        if (queue.nonEmpty) {
          val (nextCommand, nextCallback) = queue.dequeue()
          currentRunning.incrementAndGet()
          execute(nextCommand, nextCallback)
        }
      }
    })

    process.start()
  }

  def getStatus: String = synchronized {
    s"Running: ${currentRunning.get()}, In Queue: ${queue.size}, Total Processed: $totalProcessed"
  }
}

```


#### Short summary: 

empty definition using pc, found symbol in pc: java/util/concurrent/atomic/AtomicInteger#