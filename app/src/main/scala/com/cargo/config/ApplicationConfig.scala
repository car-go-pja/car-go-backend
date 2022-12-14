package com.cargo.config

import zio.config._
import zio.config.typesafe._
import zio.config.magnolia._
import ConfigDescriptor._

final case class ApplicationConfig(
    server: ServerConfig,
    database: DatabaseConfig,
    token: TokenConfig,
    sendgridConfig: SendGridConfig
)

object ApplicationConfig {
  val configuration = nested("server")(descriptor[ServerConfig])
    .zip(nested("database")(descriptor[DatabaseConfig]))
    .zip(nested("token")(TokenConfig.configuration))
    .zip(nested("sendgrid")(descriptor[SendGridConfig]))
    .to[ApplicationConfig]

  val live = TypesafeConfig.fromResourcePath(configuration)
}
