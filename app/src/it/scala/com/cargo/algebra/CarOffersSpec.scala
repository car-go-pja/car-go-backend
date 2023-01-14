package com.cargo.algebra

import zio.config.syntax._
import com.cargo.algebra.TestUtils._
import com.cargo.config.ApplicationConfig
import com.cargo.error.ApplicationError.NotAnOwner
import com.cargo.infrastructure.DatabaseTransactor
import com.cargo.model.CarOffer
import com.cargo.repository.{CarOffersRepository, ReservationsRepository, UsersRepository}
import zio._
import zio.stream.ZStream
import zio.test.Assertion.{equalTo, fails}
import zio.test._

import java.util.UUID

object CarOffersSpec extends ZIOSpecDefault {
  override def spec =
    suite("CarOffersSpec")(
      test("should add new offer") {
        for {
          token <- Authentication.login("cargo@email.com", "cargo")
          before <- CarOffers.list(None, None, None, List.empty[String])
          _ <- CarOffers.add(
            "gm",
            "aveo",
            "2010",
            100.0,
            "84",
            "gas",
            List("ac", "bluetooth"),
            "Gdansk",
            "5",
            None
          )(token.encodedToken)
          after <- CarOffers.list(None, None, None, List.empty[String])
        } yield assertTrue(before.size == 1, after.size == 2)
      },
      test("should find offer by city") {
        for {
          offers <- CarOffers.list(None, None, Some("Warszawa"), List.empty[String])
        } yield assertTrue(offers.size == 1, offers.head.city == "Warszawa")
      },
      test("should find offer by features") {
        val expectedFeatures = List("four_by_four", "ac")
        for {
          offers <- CarOffers.list(None, None, None, expectedFeatures)
        } yield assertTrue(offers.size == 1, offers.head.features.exists(expectedFeatures.contains))
      },
      test("should add images") {
        for {
          token <- Authentication.login("cargo@email.com", "cargo")
          offerId = CarOffer.Id(UUID.fromString("f53a8a80-94b0-4aab-9ef0-36a53befe69e"))
          _ <- CarOffers.addImage(ZStream.fromZIO(ZIO.attempt(1024.toByte)), offerId)(
            token.encodedToken
          )
          offer <- ZIO.serviceWithZIO[CarOffersRepository](_.get(offerId))
          _ <- CarOffers.addImage(ZStream.fromZIO(ZIO.attempt(1024.toByte)), offerId)(
            token.encodedToken
          )
          offerAppended <- ZIO.serviceWithZIO[CarOffersRepository](_.get(offerId))
        } yield assertTrue(
          offer.get.imageUrls.head.contains(s"offers/${offerId.value}"),
          offerAppended.get.imageUrls.size == 2
        )
      },
      test("should fail to add images if it's not your offer") {
        for {
          token <- Authentication.login("cargo@other.com", "cargo")
          offerId = CarOffer.Id(UUID.fromString("f53a8a80-94b0-4aab-9ef0-36a53befe69e"))
          assertion <- assertZIO(
            CarOffers
              .addImage(ZStream.fromZIO(ZIO.attempt(1024.toByte)), offerId)(
                token.encodedToken
              )
              .exit
          )(fails(equalTo(NotAnOwner)))
        } yield assertion
      },
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
      CarOffers.live,
      stubS3,
      storageCfgLayer,
      Reservations.live,
      ReservationsRepository.live,
      UserManager.live
    ) @@ TestAspect.silentLogging @@ TestAspect.sequential
}
