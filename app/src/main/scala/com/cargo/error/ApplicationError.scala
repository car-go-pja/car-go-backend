package com.cargo.error

sealed trait ApplicationError

object ApplicationError {
  final case object UserNotFound extends ApplicationError
  final case class DatabaseError(msg: String) extends ApplicationError
  final case class UnexpectedError(msg: String) extends ApplicationError
  final case object InvalidPassword extends ApplicationError
}
