package com.cargo

import com.cargo.api.AuthController
import com.cargo.api.generated.Resource
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.server.Router
import zio.ZIOAppDefault
import zio._
import zio.interop.catz._
import org.http4s.implicits._
import zio.logging.LogFormat
import zio.logging.backend.SLF4J

object Main extends ZIOAppDefault {

  override def run: ZIO[Environment with ZIOAppArgs with Scope, Any, Any] =
    app.provide(SLF4J.slf4j(LogFormat.colored))

  lazy val app =
    ZIO.runtime
      .flatMap { implicit r: Runtime[Any] =>
        val api = Router("/" -> new Resource[Task]().routes(new AuthController)).orNotFound

        BlazeServerBuilder[Task]
          .withHttpApp(api)
          .bindHttp(8081, "0.0.0.0")
          .serve
          .compile
          .drain
      }
}
