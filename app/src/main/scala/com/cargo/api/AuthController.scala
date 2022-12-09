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
            case ApplicationError.DatabaseError        => ZIO.fail(new Throwable("database error"))
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

  override def verifyEmail(
      respond: Resource.VerifyEmailResponse.type
  )(code: String, authorization: String): RIO[Authentication, Resource.VerifyEmailResponse] =
    Authentication.verifyEmail(code)(authorization).as(respond.NoContent).catchAll {
      case ApplicationError.DatabaseError   => ZIO.fail(new Throwable("database error"))
      case ApplicationError.UserNotFound    => ZIO.succeed(respond.Forbidden(Json.obj()))
      case ApplicationError.InvalidCode     => ZIO.succeed(respond.Unauthorized)
      case ApplicationError.InvalidToken(_) => ZIO.succeed(respond.Unauthorized)
      case _                                => ZIO.fail(new Throwable("other failure"))
    }

  override def getUser(respond: Resource.GetUserResponse.type)(
      authorization: String
  ): RIO[Authentication, Resource.GetUserResponse] =
    Authentication
      .getUserInfo(authorization)
      .map(user => respond.Ok(UserInfo(user.id.toString, user.email, user.isVerified)))
      .catchAll {
        case ApplicationError.DatabaseError   => ZIO.fail(new Throwable("database error"))
        case ApplicationError.InvalidCode     => ZIO.succeed(respond.Unauthorized(Json.obj()))
        case ApplicationError.InvalidToken(_) => ZIO.succeed(respond.Unauthorized(Json.obj()))
        case _                                => ZIO.fail(new Throwable("other failure"))
      }
}
