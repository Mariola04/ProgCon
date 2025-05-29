error id: file://<WORKSPACE>/server/src/main/scala/cp/serverPr/Routes.scala:java/lang/String#
file://<WORKSPACE>/server/src/main/scala/cp/serverPr/Routes.scala
empty definition using pc, found symbol in pc: java/lang/String#
empty definition using semanticdb

found definition using fallback; symbol String
offset: 260
uri: file://<WORKSPACE>/server/src/main/scala/cp/serverPr/Routes.scala
text:
```scala
package cp.serverPr

import cats.effect._
import org.http4s._
import org.http4s.dsl.io._

object Routes {

  val routes: IO[HttpRoutes[IO]] = IO {
    HttpRoutes.of[IO] {

      case req @ POST -> Root / "run" =>
        for {
          command <- req.as[Strin@@g]
          output  <- IO.async_[Response[IO]] { cb =>
            ServerState.runCommand(command, result =>
              cb(Right(Response[IO](status = Ok).withEntity(result)))
            )
          }
        } yield output

      case GET -> Root / "status" =>
        Ok(ServerState.getStatus)
    }
  }
}

```


#### Short summary: 

empty definition using pc, found symbol in pc: java/lang/String#