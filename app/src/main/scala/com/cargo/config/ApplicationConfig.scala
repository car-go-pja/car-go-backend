package com.cargo.config

import zio.config._
import zio.config.typesafe._
import zio.config.magnolia._
import ConfigDescriptor._

final case class ApplicationConfig(server: ServerConfig, database: DatabaseConfig)

object ApplicationConfig {
  val configuration = nested("server")(descriptor[ServerConfig])
    .zip(nested("database")(descriptor[DatabaseConfig]))
    .to[ApplicationConfig]

  val live = TypesafeConfig.fromResourcePath(configuration)
}
