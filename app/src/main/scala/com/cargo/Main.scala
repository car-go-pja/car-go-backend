package com.cargo

import com.cargo.api.AuthController
import com.cargo.api.generated.Resource
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.server.Router
import zio.Clock.ClockLive
import zio.ZIOAppDefault
import zio._
import zio.interop.catz._
import org.http4s.implicits._
import zio.logging.LogFormat
import zio.logging.backend.SLF4J

object Main extends ZIOAppDefault {

  override def run: ZIO[Environment with ZIOAppArgs with Scope, Any, Any] = app.zipLeft(ZIO.logInfo("app started"))

  val loggingLayer = SLF4J.slf4j(LogFormat.colored)
  val api = Router("/" -> new Resource[RIO[Clock, *]]().routes(new AuthController[Clock])).orNotFound

  lazy val app =
    ZIO.runtime.flatMap { implicit r: Runtime[Clock] =>
      BlazeServerBuilder[RIO[Clock, *]]
        .withHttpApp(api)
        .bindHttp(8081, "localhost")
        .serve
        .compile
        .drain
    }.provide(ZLayer.succeed(ClockLive) ++ loggingLayer)
}
