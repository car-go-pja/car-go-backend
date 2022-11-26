package com.cargo.model

import enumeratum.EnumEntry.Snakecase
import enumeratum._

sealed abstract class TokenType(val namespace: String) extends EnumEntry with Snakecase

object TokenType extends Enum[TokenType] {
  final case object AccessToken extends TokenType("access_claims")
  final case object VerificationToken extends TokenType("verification_claims")

  override def values: IndexedSeq[TokenType] = findValues
}
