error id: file://<WORKSPACE>/server/src/main/scala/cp/serverPr/Routes.scala:cp/serverPr/ServerState#
file://<WORKSPACE>/server/src/main/scala/cp/serverPr/Routes.scala
empty definition using pc, found symbol in pc: cp/serverPr/ServerState#
empty definition using semanticdb
empty definition using fallback
non-local guesses:
	 -org/http4s/ServerState#
	 -org/http4s/dsl/io/ServerState#
	 -ServerState#
	 -scala/Predef.ServerState#
offset: 212
uri: file://<WORKSPACE>/server/src/main/scala/cp/serverPr/Routes.scala
text:
```scala
package cp.serverPr

import cats.effect.IO
import org.http4s._
import org.http4s.dsl.io._
import scala.concurrent.{Future}

object Routes {
  print("----------------------------ROUTES")
  private val state = new @@ServerState(maxConcurrent = 3)

  val routes: IO[HttpRoutes[IO]] = IO {
    HttpRoutes.of[IO] {

      case GET -> Root / "status" =>
        Ok(state.toHtml)
          .map(addCORSHeaders)
          .map(_.withContentType(org.http4s.headers.`Content-Type`(MediaType.text.html)))

      case req @ GET -> Root / "run-process" =>
        val cmdOpt = req.uri.query.params.get("cmd")
        println(cmdOpt)
        val userIp = req.remoteAddr.map(_.toString).getOrElse("unknown")

        cmdOpt match {
          case Some(cmd) =>
            Console.print("Command: ")
            Console.println(cmd)
            Console.flush()
            val resultFuture: Future[String] = state.runProcessAsync(cmd, userIp)
            IO.fromFuture(IO(resultFuture)).flatMap { result =>
              Ok(result).map(addCORSHeaders)
            }

          case None =>
            BadRequest("Command not provided. Use /run-process?cmd=<your_command>")
              .map(addCORSHeaders)
        }
    }
  }

  def addCORSHeaders(response: Response[IO]): Response[IO] = {
    response.putHeaders(
      "Access-Control-Allow-Origin" -> "*",
      "Access-Control-Allow-Methods" -> "GET, POST, PUT, DELETE, OPTIONS",
      "Access-Control-Allow-Headers" -> "Content-Type, Authorization",
      "Access-Control-Allow-Credentials" -> "true"
    )
  }
}

```


#### Short summary: 

empty definition using pc, found symbol in pc: cp/serverPr/ServerState#