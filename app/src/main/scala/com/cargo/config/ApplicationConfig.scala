package com.cargo.config

import zio.config._
import zio.config.typesafe._
import zio.config.magnolia._
import ConfigDescriptor._

final case class ApplicationConfig(
    server: ServerConfig,
    database: DatabaseConfig,
    token: TokenConfig,
    sendgridConfig: SendGridConfig,
    storage: StorageConfig,
    twilio: TwilioConfig
)

object ApplicationConfig {
  val configuration = nested("server")(ServerConfig.configuration)
    .zip(nested("database")(descriptor[DatabaseConfig]))
    .zip(nested("token")(TokenConfig.configuration))
    .zip(nested("sendgrid")(descriptor[SendGridConfig]))
    .zip(nested("storage")(descriptor[StorageConfig]))
    .zip(nested("twilio")(descriptor[TwilioConfig]))
    .to[ApplicationConfig]

  val live = TypesafeConfig.fromResourcePath(configuration)
}
