package com.cargo.algebra

import com.cargo.error.ApplicationError
import com.cargo.error.ApplicationError.{DatabaseError, InvalidPassword, UserNotFound}
import com.cargo.model.Token
import com.password4j.Password
import com.cargo.repository.UsersRepository
import zio.Clock.ClockLive
import zio._

import java.util.UUID

trait Authentication {
  def registerUser(
      email: String,
      password: String
  ): Task[Unit] //docelowo zwraca 2fa numer i wysyla maila
  def verifyEmail(email: String, code: String): IO[ApplicationError, Unit]
  def login(email: String, password: String): IO[ApplicationError, Token]
}

object Authentication {

  def registerUser(email: String, password: String): ZIO[Authentication, Throwable, Unit] =
    ZIO.serviceWithZIO[Authentication](_.registerUser(email, password))

  def login(
      email: String,
      password: String
  ): ZIO[Authentication, ApplicationError, Token] =
    ZIO.serviceWithZIO[Authentication](_.login(email, password))

  final case class AuthLive(tokens: Tokens, users: UsersRepository) extends Authentication {
    override def registerUser(email: String, password: String): Task[Unit] =
      for {
        _ <- ZIO.logInfo("Register user request")
        id <- ZIO.succeed(UUID.randomUUID())
        _ <- users.create(id, email, Password.hash(password).withBcrypt.getResult)
        _ <- ZIO.logInfo(s"Successfully created user with id: $id")
      } yield ()

    override def verifyEmail(email: String, code: String): IO[ApplicationError, Unit] = ???

    override def login(email: String, password: String): IO[ApplicationError, Token] =
      for {
        userO <- users.find(email).mapError(err => DatabaseError(err.getMessage))
        user <- ZIO.fromOption(userO).orElseFail(UserNotFound)
        _ <- ZIO.logInfo(s"Found user with id: ${user.id}")
        _ <- ZIO.fail(InvalidPassword).unless(Password.check(password, user.password).withBcrypt)
        now <- ClockLive.instant
        token <- tokens.issueAccessToken(user.email, now)
        _ <- ZIO.logInfo(s"Successfully issued access token")
      } yield token
  }

  val live = ZLayer.fromFunction(AuthLive.apply _)
}
