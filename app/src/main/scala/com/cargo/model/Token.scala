package com.cargo.model

import pdi.jwt.JwtClaim

import java.time.Instant
import java.util.UUID
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration

final case class Token(
    id: UUID,
    encodedToken: String,
    claims: JwtClaim,
    subject: String,
    tpe: TokenType,
    expiresAt: Instant,
    issuedAt: Instant
) {
  def ttl: FiniteDuration =
    FiniteDuration(expiresAt.toEpochMilli - issuedAt.toEpochMilli, TimeUnit.MILLISECONDS)
}
