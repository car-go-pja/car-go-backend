package com.cargo.algebra

import com.cargo.config.TokenConfig
import com.cargo.model.{Token, TokenType}
import pdi.jwt._
import cats.syntax.option._
import com.cargo.error.ApplicationError.InvalidToken
import com.cargo.model.TokenType.{AccessToken, VerificationToken}
import io.circe.Json
import zio._

import java.time.Instant
import java.util.UUID

trait Tokens {
  def issueAccessToken(subject: String, at: Instant): UIO[Token]
  def issueVerificationToken(subject: String, at: Instant): Task[Token]
  def verify(rawToken: String): IO[InvalidToken, Token]
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
        token = Token(id, encodedToken, jwtClaims, subject, AccessToken, expiresAt, at)
      } yield token

    override def issueVerificationToken(subject: String, at: Instant): Task[Token] =
      for {
        expiresAt <- ZIO.succeed(at.plusMillis(config.tokenTtl.toMillis))
        id <- ZIO.succeed(UUID.randomUUID())
        jwtClaims = JwtClaim(
          content = Json.obj("tpe" -> Json.fromString(VerificationToken.namespace)).toString,
          issuer = "car-go".some,
          subject = subject.some,
          expiration = expiresAt.getEpochSecond.some,
          issuedAt = at.getEpochSecond.some,
          jwtId = id.toString.some
        )
        encodedToken = JwtCirce.encode(jwtClaims, config.privateKey, JwtAlgorithm.RS256)
        token = Token(id, encodedToken, jwtClaims, subject, VerificationToken, expiresAt, at)
      } yield token

    override def verify(rawToken: String): IO[InvalidToken, Token] =
      for {
        jwtClaims <-
          ZIO
            .fromTry(JwtCirce.decode(rawToken, config.publicKey, Seq(JwtAlgorithm.RS256)))
            .mapError(err => InvalidToken(err.getMessage))
        subject <-
          ZIO
            .fromOption(jwtClaims.subject)
            .orElseFail(InvalidToken("missing subject"))
        content <-
          ZIO
            .fromEither(io.circe.parser.parse(jwtClaims.content))
            .mapError(err => InvalidToken(err.message))
        id <-
          ZIO
            .fromOption(jwtClaims.jwtId.map(UUID.fromString))
            .orElseFail(InvalidToken("missing id"))
        tokenType <-
          ZIO
            .fromOption(
              content.asObject
                .flatMap(_.values.headOption.flatMap(_.asString))
                .map(TokenType.fromNamespace)
            )
            .orElseFail(InvalidToken("missing tpe"))
            .tapError(_ => ZIO.logInfo(s"\ndupa dupa ${content.asObject}\n"))
        expiresAt <-
          ZIO
            .fromOption(jwtClaims.expiration.map(Instant.ofEpochSecond))
            .orElseFail(InvalidToken("missing expires_at"))
        issuedAt <-
          ZIO
            .fromOption(jwtClaims.issuedAt.map(Instant.ofEpochSecond))
            .orElseFail(InvalidToken("missing issued_at"))
      } yield Token.apply(id, rawToken, jwtClaims, subject, tokenType, expiresAt, issuedAt)

  }
  val live = ZLayer.fromFunction(TokensLive.apply _)
}
