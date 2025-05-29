error id: file://<WORKSPACE>/server/src/main/scala/cp/serverPr/ServerState.scala:[13..18) in Input.VirtualFile("file://<WORKSPACE>/server/src/main/scala/cp/serverPr/ServerState.scala", "package cp.

class ServerStateRace(maxConcurrent: Int) {
  private var counter: Int = 0 // NÃO VOLÁTIL E NÃO SINCRONIZADO
  private val sem = new Semaphore(maxConcurrent)
  private val queue = new mutable.Queue[CommandRequest]()
  private val resultQueue = new mutable.Queue[CommandRequest]()

  private def nextId(): Int = {
    // AQUI: sem sincronização, pode gerar ids repetidos
    counter += 1
    counter
  }

  private val executorThread = new Thread(() => {
    while (true) {
      val requestOpt = if (queue.nonEmpty) Some(queue.dequeue()) else None // sem lock

      requestOpt.foreach { req =>
        Future {
          blocking { sem.acquire() }
          val output = new StringBuilder
          val logger = ProcessLogger(line => { output.append(line + "\n"); () })

          val shellCommand = Seq("sh", "-c", req.cmd)
          val result = Try {
            shellCommand.!(logger)
            val timestamp = java.time.LocalDateTime.now()
            s"[${req.id}] Result from '${req.cmd}' for user ${req.userIp}:\n${output}\nFinished at: $timestamp"
          }.recover {
            case e: Exception =>
              val timestamp = java.time.LocalDateTime.now()
              s"[${req.id}] Error running '${req.cmd}': ${e.getMessage}\nFinished at: $timestamp"
          }.get

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

    // AQUI: sem sincronização ao acessar as filas partilhadas
    queue.enqueue(req)
    resultQueue.enqueue(req)

    def waitForTurn(): Future[String] = Future {
      var done = false
      var result = ""
      while (!done) {
        blocking {
          if (resultQueue.headOption.contains(req) && promise.isCompleted) {
            result = promise.future.value.get.get
            resultQueue.dequeue()
            done = true
          }
        }
        if (!done) Thread.sleep(5)
      }
      result
    }

    waitForTurn()
  }
}
")
file://<WORKSPACE>/file:<WORKSPACE>/server/src/main/scala/cp/serverPr/ServerState.scala
file://<WORKSPACE>/server/src/main/scala/cp/serverPr/ServerState.scala:3: error: expected identifier; obtained class
class ServerStateRace(maxConcurrent: Int) {
^
#### Short summary: 

expected identifier; obtained class