package com.cargo.algebra

import com.cargo.config.TokenConfig
import com.cargo.model.Token
import pdi.jwt._
import zio._

import java.time.Instant
import java.util.UUID

trait Tokens {
  def issueAccessToken(subject: String, at: Instant): Task[Token]
  def issueVerificationToken(subject: String, at: Instant): Task[Token]
  def verify(rawToken: String): IO[Throwable, Unit]
}

object Tokens {
  final case class TokensLive(config: TokenConfig) extends Tokens {
    override def issueAccessToken(subject: String, at: Instant): Task[Token] =
      for {
        expiresAt <- ZIO.succeed(at.plusMillis(config.tokenTtl.toMillis))
        id <- ZIO.succeed(UUID.randomUUID())
        jwtClaims = JwtClaim()
       } yield ???


    override def issueVerificationToken(subject: String, at: Instant): Task[Token] = ???

    override def verify(rawToken: String): IO[Throwable, Unit] = ???
  }
}
