package com.cargo.algebra

import zio.config.syntax._
import com.cargo.algebra.TestUtils._
import cats.syntax.option._
import com.cargo.config.ApplicationConfig
import com.cargo.error.ApplicationError.{CarUnavailable, InsufficientBalance, OwnerSelfRent}
import com.cargo.infrastructure.DatabaseTransactor
import com.cargo.model.CarOffer
import com.cargo.repository.{CarOffersRepository, ReservationsRepository, UsersRepository}

import java.time.{LocalDate, ZoneOffset}
import zio._
import zio.test.Assertion.{equalTo, fails}
import zio.test._

import java.util.UUID

object UserManagerSpec extends ZIOSpecDefault {
  override def spec: Spec[TestEnvironment with Scope, Any] =
    suite("UserManagerSpec")(
      test("should update profile") {
        for {
          token <- Authentication.login("cargo@other.com", "cargo")
          firstName = "John".some
          lastName = "Doe".some
          dob = LocalDate.of(2000, 1, 1).some
          _ <- UserManager.updateProfile(firstName, lastName, None, dob, None)(token.encodedToken)
          user <- ZIO.serviceWithZIO[UsersRepository](_.find("cargo@other.com"))
        } yield assertTrue(user.get.firstName == firstName, user.get.lastName == lastName, user.get.dob == dob)
      }
    ).provideShared(
      Authentication.live,
      embeddedPostgresLayer.orDie,
      Tokens.live,
      mockedEmailNotification,
      UsersRepository.live,
      DatabaseTransactor.live,
      dbConfigLayer,
      ApplicationConfig.live.narrow(_.token),
      CarOffersRepository.live,
      Reservations.live,
      ReservationsRepository.live,
      UserManager.live
    ) @@ TestAspect.silentLogging @@ TestAspect.sequential
}
