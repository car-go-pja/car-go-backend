package com.cargo.algebra

import com.cargo.error.ApplicationError
import com.cargo.error.ApplicationError._
import com.cargo.model.TokenType.VerificationToken
import com.cargo.model.{Token, User}
import com.password4j.Password
import com.cargo.repository.UsersRepository
import com.cargo.service.Notifications
import zio.Clock.ClockLive
import zio._

import java.time.Instant
import scala.util.Random
import java.util.UUID

trait Authentication {
  def registerUser(email: String, password: String): IO[ApplicationError, Token]
  def verifyEmail(code: String)(rawToken: String): IO[ApplicationError, Unit]
  def login(email: String, password: String): IO[ApplicationError, Token]
  def getUserInfo(rawToken: String): IO[ApplicationError, User]
  def sendResetPassword(email: String): IO[ApplicationError, Unit]
}

object Authentication {

  def registerUser(email: String, password: String): ZIO[Authentication, ApplicationError, Token] =
    ZIO.serviceWithZIO[Authentication](_.registerUser(email, password))

  def login(
      email: String,
      password: String
  ): ZIO[Authentication, ApplicationError, Token] =
    ZIO.serviceWithZIO[Authentication](_.login(email, password))

  def verifyEmail(code: String)(
      rawToken: String
  ): ZIO[Authentication, ApplicationError, Unit] =
    ZIO.serviceWithZIO[Authentication](_.verifyEmail(code)(rawToken))

  def getUserInfo(rawToken: String): ZIO[Authentication, ApplicationError, User] =
    ZIO.serviceWithZIO[Authentication](_.getUserInfo(rawToken))

  def sendResetPassword(email: String): ZIO[Authentication, ApplicationError, Unit] =
    ZIO.serviceWithZIO(_.sendResetPassword(email))

  final case class AuthLive(
      tokens: Tokens,
      users: UsersRepository,
      emailNotification: Notifications
  ) extends Authentication {
    override def registerUser(email: String, password: String): IO[ApplicationError, Token] =
      for {
        _ <- ZIO.logInfo("Register user request")
        userId <- ZIO.succeed(User.Id(UUID.randomUUID()))
        _ <- users.create(userId, email, Password.hash(password).withBcrypt.getResult)
        _ <- ZIO.logInfo("User successfully saved to db")
        verificationId <- ZIO.succeed(UUID.randomUUID())
        createdAt <- ZIO.succeed(Instant.now())
        verificationCode = Random.between(100000, 1000000).toString
        _ <- users.saveVerificationCode(verificationId, verificationCode, userId, createdAt)
        _ <- ZIO.logInfo(s"Successfully created user with id: $userId")
        _ <- emailNotification.sendVerificationEmail(email, verificationCode)
        token <-
          tokens
            .issueVerificationToken(email, createdAt)
            .mapError(err => UnexpectedError(err.getMessage))
      } yield token

    override def verifyEmail(code: String)(
        rawToken: String
    ): IO[ApplicationError, Unit] =
      for {
        _ <- ZIO.logInfo("Verify email request")
        token <- tokens.verify(rawToken)
        _ <- ZIO.unless(token.tpe == VerificationToken)(ZIO.fail(InvalidToken("wrong token tpe")))
        userO <- users.find(token.subject).orElseFail(DatabaseError)
        user <- ZIO.fromOption(userO).orElseFail(UserNotFound)
        verificationO <- users.findVerificationRow(user.id).orElseFail(DatabaseError)
        isVerified <-
          ZIO.fromOption(verificationO).mapBoth(_ => UnexpectedError(""), _.code == code)
        _ <- ZIO.unless(isVerified)(ZIO.fail(InvalidCode))
        _ <- users.markAsVerified(user.id).orElseFail(DatabaseError)
        _ <- ZIO.logInfo(s"Successfully verified user: ${user.email}")
      } yield ()

    override def login(email: String, password: String): IO[ApplicationError, Token] =
      for {
        _ <- ZIO.logInfo("Login request")
        userO <- users.find(email).orElseFail(DatabaseError)
        user <- ZIO.fromOption(userO).orElseFail(UserNotFound)
        _ <- ZIO.logInfo(s"Found user with id: ${user.id}")
        _ <- ZIO.fail(InvalidPassword).unless(Password.check(password, user.password).withBcrypt)
        now <- ClockLive.instant
        token <- tokens.issueAccessToken(user.email, now)
        _ <- ZIO.logInfo(s"Successfully issued access token")
      } yield token

    override def getUserInfo(rawToken: String): IO[ApplicationError, User] =
      for {
        _ <- ZIO.logInfo("Get user info request")
        user <- getUser(rawToken)(tokens, users)
        _ <- ZIO.logInfo("Successfully sent user info")
      } yield user

    override def sendResetPassword(email: String): IO[ApplicationError, Unit] =
      for {
        userO <- users.find(email).orElseFail(DatabaseError)
        user <- ZIO.fromOption(userO).orElseFail(UserNotFound)
        code <- ZIO.succeed(Random.alphanumeric.take(30).mkString)
        _ <- users.setResetToken(user.id, code)
        _ <- emailNotification.sendResetLink(email, code)
      } yield ()
  }

  val live = ZLayer.fromFunction(AuthLive.apply _)
}
