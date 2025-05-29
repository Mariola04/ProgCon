error id: file://<WORKSPACE>/server/src/main/scala/cp/serverPr/Routes.scala:cp/serverPr/`<error: <none>>`.
file://<WORKSPACE>/server/src/main/scala/cp/serverPr/Routes.scala
empty definition using pc, found symbol in pc: 
empty definition using semanticdb
empty definition using fallback
non-local guesses:
	 -upickle/default/upickle.
	 -upickle.
	 -scala/Predef.upickle.
offset: 30
uri: file://<WORKSPACE>/server/src/main/scala/cp/serverPr/Routes.scala
text:
```scala
package cp.serverPr

import up@@ickle.default._
import cask.model.Response

object Routes extends cask.MainRoutes {

  @cask.post("/run")
  def runCommand(request: cask.Request): Response[String] = {
    val body = request.text()
    println(s"Received command: $body")

    var result = ""
    val latch = new Object

    ServerState.runCommand(body, output => {
      result = output
      latch.synchronized {
        latch.notify()
      }
    })

    latch.synchronized {
      latch.wait()
    }

    Response(result, headers = Seq("Access-Control-Allow-Origin" -> "*"))
  }

  @cask.get("/status")
  def getStatus(): Response[String] = {
    val status = ServerState.getStatus
    Response(status, headers = Seq("Access-Control-Allow-Origin" -> "*"))
  }

  initialize()
}

```


#### Short summary: 

empty definition using pc, found symbol in pc: 