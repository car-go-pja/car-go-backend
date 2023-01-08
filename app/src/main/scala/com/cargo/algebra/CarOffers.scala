package com.cargo.algebra

import com.cargo.config.StorageConfig
import com.cargo.error.ApplicationError
import com.cargo.error.ApplicationError.UnexpectedError
import com.cargo.model.{CarOffer, Point}
import com.cargo.repository.{CarOffersRepository, UsersRepository}
import zio._
import zio.s3._
import zio.stream.ZStream

import java.time.Instant
import java.util.UUID
import scala.util.Random

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

  def addImage(img: ZStream[Authentication with CarOffers, Throwable, Byte], offerId: CarOffer.Id)(
      rawToken: String
  ): ZIO[Authentication with CarOffers, ApplicationError, Unit]
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
    ZIO.serviceWithZIO[CarOffers](
      _.add(
        make,
        model,
        year,
        pricePerDay,
        horsepower,
        fuelType,
        features,
        city,
        seatsAmount,
        geolocation
      )(rawToken)
    )

  def list(
      from: Option[Instant],
      to: Option[Instant],
      city: Option[String],
      features: List[String]
  ): ZIO[CarOffers, ApplicationError, List[CarOffer]] =
    ZIO.serviceWithZIO[CarOffers](_.list(from, to, city, features))

  def addImage(img: ZStream[Authentication with CarOffers, Throwable, Byte], offerId: CarOffer.Id)(
      rawToken: String
  ): ZIO[Authentication with CarOffers, ApplicationError, Unit] =
    ZIO.serviceWithZIO[CarOffers](_.addImage(img, offerId)(rawToken))

  final case class CarOffersLive(
      tokens: Tokens,
      carOffersRepo: CarOffersRepository,
      usersRepository: UsersRepository,
      cfg: StorageConfig,
      s3: S3
  ) extends CarOffers {
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
        user <- getUser(rawToken)(tokens, usersRepository)
        id <- ZIO.succeed(CarOffer.Id(UUID.randomUUID()))
        _ <- carOffersRepo.create(
          id,
          user.id,
          make,
          model,
          year,
          pricePerDay,
          horsepower,
          fuelType,
          features,
          city,
          seatsAmount,
          geolocation,
          Instant.now()
        )
        _ <- ZIO.logInfo("Successfully added offer")
      } yield ()

    override def list(
        from: Option[Instant],
        to: Option[Instant],
        city: Option[String],
        features: List[String]
    ): IO[ApplicationError, List[CarOffer]] =
      carOffersRepo
        .list(from, to, city, features)
        .tap(offers => ZIO.logInfo(s"Successfully listed offers with size: ${offers.size}"))

    override def addImage(
        img: ZStream[Authentication with CarOffers, Throwable, Byte],
        offerId: CarOffer.Id
    )(rawToken: String): ZIO[Authentication with CarOffers, ApplicationError, Unit] =
      for {
        _ <- ZIO.logInfo("Add image request")
        user <- getUser(rawToken)(tokens, usersRepository)
        offerO <- carOffersRepo.get(offerId)
        offer <-
          ZIO.fromOption(offerO).orElseFail(ApplicationError.OfferNotFound(offerId.value.toString))
        _ <- ZIO.unless(user.id == offer.ownerId)(ZIO.fail(ApplicationError.NotAnOwner))
        randomKey <- ZIO.succeed(Random.alphanumeric.take(7).mkString(""))
        resource = s"offers/${offerId.value}/$randomKey.jpg"
        _ <-
          s3.multipartUpload(
            cfg.bucketName,
            resource,
            img,
            MultipartUploadOptions.fromUploadOptions(UploadOptions.fromContentType("image/jpg"))
          )(2)
            .mapError(err => UnexpectedError(err.getMessage): ApplicationError)
            .tapError(err => ZIO.logError(s"Failed to save an img: $err"))
        _ <- carOffersRepo.saveImage(
          s"https://${cfg.bucketName}.s3.eu-north-1.amazonaws.com/$resource",
          offerId
        )
        _ <- ZIO.logInfo("Successfully added image")
      } yield ()
  }

  val live = ZLayer.fromFunction(CarOffersLive.apply _)
}
