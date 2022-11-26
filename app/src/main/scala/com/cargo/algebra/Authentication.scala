package com.cargo.algebra

import com.cargo.error.ApplicationError
import zio._

trait Authentication {
  def registerUser(email: String, password: String): Task[Unit]
  def verifyEmail(email: String, code: String): IO[ApplicationError, Unit]
}

object Authentication {
  final case class AuthLive()
}
