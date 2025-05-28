package cp.serverPr

import cats.effect.IO
import org.http4s._
import org.http4s.dsl.io._

object Routes {
  private val state = new ServerState(maxConcurrent = 3)

  val routes: IO[HttpRoutes[IO]] = IO {
    HttpRoutes.of[IO] {

      case GET -> Root / "status" =>
        Ok(state.toHtml)
          .map(addCORSHeaders)
          .map(_.withContentType(org.http4s.headers.`Content-Type`(MediaType.text.html)))

      case req @ GET -> Root / "run-process" =>
        val cmdOpt = req.uri.query.params.get("cmd")
        val userIp = req.remoteAddr.map(_.toString).getOrElse("unknown")

        cmdOpt match {
          case Some(cmd) =>
            val futureResult = state.submitCommand(cmd, userIp)
            val ioResult = IO.fromFuture(IO(futureResult))
            ioResult.flatMap(res => Ok(res).map(addCORSHeaders))

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
