package com.cargo.algebra

import zio.config.syntax._
import com.cargo.algebra.TestUtils._
import com.cargo.config.ApplicationConfig
import com.cargo.infrastructure.DatabaseTransactor
import com.cargo.repository.{CarOffersRepository, UsersRepository}
import zio.test._

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
      test("should find offer by city"){
        for {
          offers <- CarOffers.list(None, None, Some("Warszawa"), List.empty[String])
        } yield assertTrue(offers.size == 1, offers.head.city == "Warszawa")
      },
      test("should find offer by features"){
        val expectedFeatures = List("four_by_four", "ac")
        for {
          offers <- CarOffers.list(None, None, None, expectedFeatures)
        } yield assertTrue(offers.size == 1, offers.head.features.exists(expectedFeatures.contains))
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
      CarOffers.live
    ) @@ TestAspect.silentLogging @@ TestAspect.sequential
}
