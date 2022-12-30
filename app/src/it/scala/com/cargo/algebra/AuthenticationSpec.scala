package com.cargo.algebra

import com.cargo.model.TokenType.VerificationToken
import com.cargo.algebra.TestUtils._
import com.cargo.config.ApplicationConfig
import com.cargo.error.ApplicationError.DatabaseError
import zio.config.syntax._
import zio.test.Assertion._
import com.cargo.infrastructure.DatabaseTransactor
import com.cargo.repository.UsersRepository
import zio.test._

object AuthenticationSpec extends ZIOSpecDefault {

  private val testEmail = "test@email.com"
  private val testPwd = "test123"

  def spec = suite("AuthenticationSpec")(
    test("should register new user"){
      for {
        token <- Authentication.registerUser(testEmail, testPwd)
      } yield assertTrue(token.tpe == VerificationToken)
    },
    test("should fail to register user with the same email"){
      assertZIO(Authentication.registerUser(testEmail, testPwd).exit)(fails(equalTo(DatabaseError)))
    },
    // should be able to verify email and login
    // should fail to log in for invalid password
  ).provideShared(
    Authentication.live,
    embeddedPostgresLayer.orDie,
    Tokens.live,
    mockedEmailNotification,
    UsersRepository.live,
    DatabaseTransactor.live,
    dbConfigLayer,
    ApplicationConfig.live.narrow(_.token)
  ) @@ TestAspect.silentLogging @@ TestAspect.sequential
}
