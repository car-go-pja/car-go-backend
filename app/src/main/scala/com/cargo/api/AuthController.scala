package com.cargo.api

import com.cargo.algebra.Authentication
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
          .catchAll {
            case ApplicationError.DatabaseError(msg)   => ZIO.fail(new Throwable(msg))
            case ApplicationError.UserNotFound         => ZIO.fail(new Throwable(""))
            case ApplicationError.UnexpectedError(msg) => ZIO.fail(new Throwable(msg))
          }
      case None => ZIO.succeed(respond.Unauthorized(Json.obj()))
    }
  override def registerUser(respond: Resource.RegisterUserResponse.type)(
      body: Option[UserCredentials]
  ): RIO[Authentication, Resource.RegisterUserResponse] =
    body match {
      case Some(credentials) =>
        Authentication
          .registerUser(credentials.email, credentials.password)
          .as(respond.Created)
      case None => ZIO.succeed(respond.Unauthorized(Json.obj()))
    }

  override def verifyEmail(respond: Resource.VerifyEmailResponse.type)(
      secret: Option[String]
  ): RIO[Authentication, Resource.VerifyEmailResponse] = ???

  override def getUser(
      respond: Resource.GetUserResponse.type
  )(): RIO[Authentication, Resource.GetUserResponse] =
    ???
}
