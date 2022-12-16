package com.cargo.config

import zio.config._

import scala.concurrent.duration.Duration

final case class TokenConfig(secret: String, tokenTtl: Duration)

object TokenConfig {
  implicit val configuration: ConfigDescriptor[TokenConfig] =
    ConfigDescriptor
      .string("secret")
      .zip(ConfigDescriptor.duration("timeToLive"))
      .map {
        case (secret, tokenTtl) => TokenConfig(secret, tokenTtl)
      }
}
