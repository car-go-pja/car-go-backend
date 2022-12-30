package com.cargo.algebra

import com.cargo.error.ApplicationError
import com.cargo.error.ApplicationError.UserNotFound
import com.cargo.model.{CarOffer, Point}
import com.cargo.repository.{CarOffersRepository, UsersRepository}
import zio._

import java.time.Instant
import java.util.UUID

trait CarOffers {
  def add(
    make: String,
    model: String,
    year: String,
    pricePerDay: BigDecimal,
    horsepower: String,
    fuelType: String,
    features: List[String],
    city: String,
    seatsAmount: String,
    geolocation: Option[Point]
  )(rawToken: String): IO[ApplicationError, Unit]
  def list(
    from: Option[Instant],
    to: Option[Instant],
    city: Option[String],
    features: List[String]
  ): IO[ApplicationError, List[CarOffer]]
}

object CarOffers {
  def add(
    make: String,
    model: String,
    year: String,
    pricePerDay: BigDecimal,
    horsepower: String,
    fuelType: String,
    features: List[String],
    city: String,
    seatsAmount: String,
    geolocation: Option[Point]
  )(rawToken: String): ZIO[CarOffers, ApplicationError, Unit] =
    ZIO.serviceWithZIO[CarOffers](_.add(make, model, year, pricePerDay, horsepower, fuelType, features, city, seatsAmount, geolocation)(rawToken))

  def list(
    from: Option[Instant],
    to: Option[Instant],
    city: Option[String],
    features: List[String]
  ): ZIO[CarOffers, ApplicationError, List[CarOffer]] =
    ZIO.serviceWithZIO[CarOffers](_.list(from, to, city, features))

  final case class CarOffersLive(tokens: Tokens, carOffersRepo: CarOffersRepository, usersRepository: UsersRepository) extends CarOffers {
    override def add(
      make: String,
      model: String,
      year: String,
      pricePerDay: BigDecimal,
      horsepower: String,
      fuelType: String,
      features: List[String],
      city: String,
      seatsAmount: String,
      geolocation: Option[Point]
    )(rawToken: String): IO[ApplicationError, Unit] =
      for {
        _ <- ZIO.logInfo("Add car offer request")
        token <- tokens.verify(rawToken)
        userO <- usersRepository.find(token.subject)
        user <- ZIO.fromOption(userO).orElseFail(UserNotFound)
        id <- ZIO.succeed(CarOffer.Id(UUID.randomUUID()))
        _ <- carOffersRepo.create(id, user.id, make, model, year, pricePerDay, horsepower, fuelType, features, city, seatsAmount, geolocation, Instant.now())
        _ <- ZIO.logInfo("Successfully added offer")
      } yield ()

    override def list(from: Option[Instant], to: Option[Instant], city: Option[String], features: List[String]): IO[ApplicationError, List[CarOffer]] =
      carOffersRepo.list(from, to, city, features).tap(offers => ZIO.logInfo(s"Successfully listed offers with size: ${offers.size}"))
  }

  val live = ZLayer.fromFunction(CarOffersLive.apply _)
}
