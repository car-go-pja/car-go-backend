package com.cargo.config

import org.typelevel.ci.CIString
import org.http4s.implicits._
import zio.config._

final case class ServerConfig(hostname: String, port: Int, allowedOrigins: Set[CIString])

object ServerConfig {
  implicit val configuration: ConfigDescriptor[ServerConfig] =
    ConfigDescriptor
      .string("hostname")
      .zip(ConfigDescriptor.int("port"))
      .zip(ConfigDescriptor.string("allowedOrigins"))
      .map {
        case (hostname, port, allowedOrg) =>
          ServerConfig(hostname, port, allowedOrg.split(",").map(_.ci).toSet)
      }
}
