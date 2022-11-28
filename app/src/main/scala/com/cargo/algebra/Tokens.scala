package com.cargo.algebra

import com.cargo.config.TokenConfig
import com.cargo.model.Token
import pdi.jwt._
import cats.syntax.option._
import com.cargo.model.TokenType.AccessToken
import io.circe.Json
import zio._

import java.time.Instant
import java.util.UUID

trait Tokens {
  def issueAccessToken(subject: String, at: Instant): UIO[Token]
  def issueVerificationToken(subject: String, at: Instant): Task[Token]
  def verify(rawToken: String): IO[Throwable, Unit]
}

object Tokens {
  final case class TokensLive(config: TokenConfig) extends Tokens {
    override def issueAccessToken(subject: String, at: Instant): UIO[Token] =
      for {
        expiresAt <- ZIO.succeed(at.plusMillis(config.tokenTtl.toMillis))
        id <- ZIO.succeed(UUID.randomUUID())
        jwtClaims = JwtClaim(
          content = Json.obj("tpe" -> Json.fromString(AccessToken.namespace)).toString,
          issuer = "car-go".some,
          subject = subject.some,
          expiration = expiresAt.getEpochSecond.some,
          issuedAt = at.getEpochSecond.some,
          jwtId = id.toString.some
        )
        encodedToken = JwtCirce.encode(jwtClaims, config.privateKey, JwtAlgorithm.RS256)
        token = Token(id, encodedToken, jwtClaims, AccessToken, expiresAt, at)
      } yield token

    override def issueVerificationToken(subject: String, at: Instant): Task[Token] = ???

    override def verify(rawToken: String): IO[Throwable, Unit] = ???
  }

  val live = ZLayer.fromFunction(TokensLive.apply _)
}
