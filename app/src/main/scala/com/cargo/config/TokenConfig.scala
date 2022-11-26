package com.cargo.config

import zio.config._

import scala.concurrent.duration.Duration
import scala.io.Source

final case class TokenConfig(publicKey: String, privateKey: String, tokenTtl: Duration)

object TokenConfig {
  implicit val configuration: ConfigDescriptor[TokenConfig] =
    ConfigDescriptor
      .string("publicKeyPath")
      .zip(ConfigDescriptor.string("privateKeyPath"))
      .map {
        case (publicKeyPath, privateKeyPath) =>
          val pubKeySrc = Source.fromFile(publicKeyPath)
          val prvKeySrc = Source.fromFile(privateKeyPath)

          try (pubKeySrc.mkString, prvKeySrc.mkString)
          finally (pubKeySrc.close(), prvKeySrc.close())
      }.zip(ConfigDescriptor.duration("timeToLive"))
      .map {
        case (pubKey, prvKey, tokenTtl) => TokenConfig(pubKey, prvKey, tokenTtl)
      }
}
