package com.cargo

import com.cargo.error.ApplicationError
import com.cargo.error.ApplicationError.{InvalidToken, UserNotFound}
import com.cargo.model.TokenType.AccessToken
import com.cargo.model.User
import com.cargo.repository.UsersRepository
import zio._

package object algebra {
  private[algebra] def getUser(
      rawToken: String
  )(tokens: Tokens, users: UsersRepository): IO[ApplicationError, User] =
    for {
      token <- tokens.verify(rawToken)
      _ <- ZIO.unless(token.tpe == AccessToken)(ZIO.fail(InvalidToken("wrong token tpe")))
      userO <- users.find(token.subject)
      user <- ZIO.fromOption(userO).orElseFail(UserNotFound)
    } yield user
}
