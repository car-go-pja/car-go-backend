package com.cargo.api

import zio._
import com.cargo.api.generated.{Handler, Resource}
import com.cargo.api.generated.definitions.dto._

final class AuthController[R] extends Handler[RIO[R, *]] {
  override def login(respond: Resource.LoginResponse.type)(body: Option[UserCredentials]): RIO[R, Resource.LoginResponse] =
    ZIO.succeed(respond.Ok(AccessToken("token"))).zipLeft(ZIO.logInfo(s"Found ${body.get.email} email"))

  override def registerUser(respond: Resource.RegisterUserResponse.type)(body: Option[UserCredentials]): RIO[R, Resource.RegisterUserResponse] = ???

  override def verifyEmail(respond: Resource.VerifyEmailResponse.type)(secret: Option[String]): RIO[R, Resource.VerifyEmailResponse] = ???

  override def getUser(respond: Resource.GetUserResponse.type)(): RIO[R, Resource.GetUserResponse] = ???
}
