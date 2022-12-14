package com.cargo.api

import com.cargo.algebra.Authentication
import cats.syntax.option._
import zio._
import com.cargo.api.generated.{Handler, Resource}
import com.cargo.api.generated.definitions.dto._
import com.cargo.error.ApplicationError
import io.circe.Json

final class AuthController extends Handler[RIO[Authentication, *]] {
  override def login(
      respond: Resource.LoginResponse.type
  )(body: Option[UserCredentials]): RIO[Authentication, Resource.LoginResponse] =
    body match {
      case Some(credentials) =>
        Authentication
          .login(credentials.email, credentials.password)
          .map(token => respond.Ok(AccessToken(token.encodedToken)))
          .catchAll(err => catchApplicationError(respond.Unauthorized)(err))
      case None => ZIO.succeed(respond.Unauthorized(ErrorResponse("invalid_credentials", None)))
    }
  override def registerUser(respond: Resource.RegisterUserResponse.type)(
      body: Option[UserCredentials]
  ): RIO[Authentication, Resource.RegisterUserResponse] =
    body match {
      case Some(credentials) =>
        Authentication
          .registerUser(credentials.email, credentials.password)
          .map(token => respond.Ok(VerificationToken(token.encodedToken.some)))
          .catchAll(_ => ZIO.fail(new Throwable("xd"))) //fixme
      case None => ZIO.succeed(respond.Unauthorized(ErrorResponse("")))
    }

  override def verifyEmail(
      respond: Resource.VerifyEmailResponse.type
  )(code: String, authorization: String): RIO[Authentication, Resource.VerifyEmailResponse] =
    Authentication
      .verifyEmail(code)(authorization.drop(7)) //fixme parse bearer token (middleware?)
      .as(respond.NoContent)
      .catchAll(err => catchApplicationError(respond.Unauthorized)(err))

  override def getUser(respond: Resource.GetUserResponse.type)(
      authorization: String
  ): RIO[Authentication, Resource.GetUserResponse] =
    Authentication
      .getUserInfo(authorization.drop(7))
      .map(user => respond.Ok(UserInfo(user.id.toString, user.email, user.isVerified)))
      .catchAll(err => catchApplicationError(respond.Unauthorized)(err))
  private def catchApplicationError[Resp](
      unauthorized: ErrorResponse => Resp
  )(error: ApplicationError): ZIO[Any, Throwable, Resp] =
    error match {
      case _: ApplicationError.DatabaseError.type =>
        ZIO.fail(new RuntimeException("database exception"))
      case ApplicationError.IntegrationError(msg) =>
        ZIO.fail(new RuntimeException(s"integration failure $msg")) //fixme 502 error
      case ApplicationError.UnexpectedError(msg) =>
        ZIO.fail(new RuntimeException(s"unexpected error: $msg"))
      case _: ApplicationError.InvalidCode.type =>
        ZIO.succeed(unauthorized(ErrorResponse("invalid_code", None)))
      case _: ApplicationError.InvalidPassword.type =>
        ZIO.succeed(unauthorized(ErrorResponse("invalid_password", None)))
      case ApplicationError.InvalidToken(msg) =>
        ZIO.succeed(unauthorized(ErrorResponse("invalid_token", msg.some)))
      case ApplicationError.UserNotFound =>
        ZIO.fail(new RuntimeException("user not found"))
    }
}
