package com.cargo.algebra

import zio.config.syntax._
import com.cargo.algebra.TestUtils._
import com.cargo.api.generated.definitions.dto.Insurance
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

object ReservationsSpec extends ZIOSpecDefault {
  override def spec: Spec[TestEnvironment with Scope, Any] =
    suite("ReservationsSpec")(
      test("should make reservation") {
        for {
          token <- Authentication.login("cargo@other.com", "cargo")
          balanceBefore <- ZIO.serviceWithZIO[UsersRepository](_.find("cargo@other.com")).map(_.get.balance)
          offerId = CarOffer.Id(UUID.fromString("f53a8a80-94b0-4aab-9ef0-36a53befe69e"))
          from = LocalDate.of(2023, 1, 19).atStartOfDay().toInstant(ZoneOffset.UTC)
          to = LocalDate.of(2023, 1, 24).atStartOfDay().toInstant(ZoneOffset.UTC)
          _ <- Reservations.makeReservation(offerId, from, to, Insurance.Medium)(token.encodedToken)
          balanceAfter <- ZIO.serviceWithZIO[UsersRepository](_.find("cargo@other.com")).map(_.get.balance)
        } yield assertTrue((balanceBefore - 2195.0) == balanceAfter)
      },
      test("should fail on renting your own car") {
        for {
          token <- Authentication.login("cargo@email.com", "cargo")
          offerId = CarOffer.Id(UUID.fromString("f53a8a80-94b0-4aab-9ef0-36a53befe69e"))
          from = LocalDate.of(2023, 1, 25).atStartOfDay().toInstant(ZoneOffset.UTC)
          to = LocalDate.of(2023, 1, 27).atStartOfDay().toInstant(ZoneOffset.UTC)
          assertion <- assertZIO(
            Reservations
              .makeReservation(offerId, from, to, Insurance.Medium)(token.encodedToken)
              .exit
          )(fails(equalTo(OwnerSelfRent)))
        } yield assertion
      },
      test("should fail on renting unavailable car") {
        for {
          token <- Authentication.login("cargo@other.com", "cargo")
          offerId = CarOffer.Id(UUID.fromString("f53a8a80-94b0-4aab-9ef0-36a53befe69e"))
          from = LocalDate.of(2023, 1, 15).atStartOfDay().toInstant(ZoneOffset.UTC)
          to = LocalDate.of(2023, 1, 20).atStartOfDay().toInstant(ZoneOffset.UTC)
          assertion <- assertZIO(
            Reservations
              .makeReservation(offerId, from, to, Insurance.Medium)(token.encodedToken)
              .exit
          )(fails(equalTo(CarUnavailable)))
        } yield assertion
      },
      test("should fail to rent a car, because of insufficient balance") {
        for {
          token <- Authentication.login("cargo@other.com", "cargo")
          offerId = CarOffer.Id(UUID.fromString("f53a8a80-94b0-4aab-9ef0-36a53befe69e"))
          from = LocalDate.of(2023, 1, 28).atStartOfDay().toInstant(ZoneOffset.UTC)
          to = LocalDate.of(2023, 2, 3).atStartOfDay().toInstant(ZoneOffset.UTC)
          assertion <- assertZIO(
            Reservations
              .makeReservation(offerId, from, to, Insurance.Medium)(token.encodedToken)
              .exit
          )(fails(equalTo(InsufficientBalance)))
        } yield assertion
      },
      test("should list user's reservations") {
        for {
          token <- Authentication.login("cargo@email.com", "cargo")
          reservations <- Reservations.list(token.encodedToken)
        } yield assertTrue(reservations.size == 2)
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
      ReservationsRepository.live
    ) @@ TestAspect.silentLogging @@ TestAspect.sequential
}
