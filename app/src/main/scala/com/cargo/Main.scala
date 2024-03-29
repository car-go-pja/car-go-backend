package com.cargo

import com.cargo.algebra.{Authentication, CarOffers, Reservations, Tokens, UserManager}
import com.cargo.api.{Infrastructure, MainController}
import com.cargo.api.generated.Resource
import com.cargo.config.{ApplicationConfig, SendGridConfig, StorageConfig, TwilioConfig}
import com.cargo.infrastructure.DatabaseTransactor
import com.cargo.repository.{CarOffersRepository, ReservationsRepository, UsersRepository}
import com.cargo.service.Notifications
import com.sendgrid.SendGrid
import com.twilio.Twilio
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.server.Router
import zio.ZIOAppDefault
import zio._
import zio.s3._
import software.amazon.awssdk.regions.Region.EU_NORTH_1
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import zio.interop.catz._
import org.http4s.server.middleware.CORS
import org.http4s.implicits._
import zio.logging.LogFormat
import zio.config.syntax._
import zio.logging.backend.SLF4J

object Main extends ZIOAppDefault {

  override def run: ZIO[Environment with ZIOAppArgs with Scope, Any, Any] =
    app.provide(
      SLF4J.slf4j(LogFormat.colored),
      DatabaseTransactor.live,
      ApplicationConfig.live,
      ApplicationConfig.live.narrow(_.token),
      ApplicationConfig.live.narrow(_.database),
      ApplicationConfig.live.narrow(_.sendgridConfig),
      ApplicationConfig.live.narrow(_.storage),
      ApplicationConfig.live.narrow(_.twilio),
      ZLayer
        .service[StorageConfig]
        .flatMap(env =>
          live(
            EU_NORTH_1,
            AwsBasicCredentials.create(env.get.accessKeyId, env.get.secret)
          )
        ),
      Authentication.live,
      UsersRepository.live,
      Tokens.live,
      ZLayer(ZIO.service[SendGridConfig].map(cfg => new SendGrid(cfg.sendGridApiKey))),
      ZLayer(
        ZIO
          .service[TwilioConfig]
          .map { conf =>
            Twilio.init(conf.accountSid, conf.authToken)
            Twilio.getRestClient
          }
      ),
      Notifications.live,
      CarOffers.live,
      CarOffersRepository.live,
      Reservations.live,
      ReservationsRepository.live,
      UserManager.live
    )

  lazy val app =
    ZIO.service[ApplicationConfig].flatMap { cfg =>
      ZIO.runtime
        .flatMap { implicit r: Runtime[Any] =>
          val cors = CORS.policy
            .withAllowCredentials(true)
            .withAllowOriginHostCi(cfg.server.allowedOrigins.contains)

          println(s"CORS config: " + cfg.server.allowedOrigins)

          val api =
            Router(
              "/" -> new Resource[RIO[Infrastructure, *]]()
                .routes(new MainController)
            ).orNotFound

          BlazeServerBuilder[RIO[Infrastructure, *]]
            .withHttpApp(cors(api))
            .bindHttp(cfg.server.port, cfg.server.hostname)
            .serve
            .compile
            .drain
        }
    }
}
