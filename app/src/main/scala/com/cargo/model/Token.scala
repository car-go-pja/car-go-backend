package com.cargo.model

import pdi.jwt.JwtClaim

import java.time.Instant
import java.util.UUID
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration

final case class Token private (id: UUID, claims: JwtClaim, tpe: TokenType, expiresAt: Instant, issuedAt: Instant) {
  def ttl: FiniteDuration = FiniteDuration(expiresAt.toEpochMilli - issuedAt.toEpochMilli, TimeUnit.MILLISECONDS)
}

object Token {
  def apply(id: UUID, claims: JwtClaim, tpe: TokenType, expiresAt: Instant, issuedAt: Instant): Option[Token] =
    Option.when(expiresAt isAfter issuedAt)(new Token(id, claims, tpe, expiresAt, issuedAt))
}
