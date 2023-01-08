package com.cargo.error

sealed trait ApplicationError

object ApplicationError {
  final case object UserNotFound extends ApplicationError
  final case class OfferNotFound(msg: String) extends ApplicationError
  final case object DatabaseError extends ApplicationError
  final case object InvalidCode extends ApplicationError
  final case object NotAnOwner extends ApplicationError
  final case class UnexpectedError(msg: String) extends ApplicationError
  final case class IntegrationError(msg: String) extends ApplicationError
  final case class InvalidToken(msg: String) extends ApplicationError
  final case object InvalidPassword extends ApplicationError
}
