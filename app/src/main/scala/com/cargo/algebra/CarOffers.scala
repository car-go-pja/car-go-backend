package com.cargo.algebra

import com.cargo.api.Infrastructure
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
  )(rawToken: String): IO[ApplicationError, CarOffer]

  def list(
      from: Option[Instant],
      to: Option[Instant],
      city: Option[String],
      features: List[String]
  ): IO[ApplicationError, List[CarOffer]]

  def addImage(img: ZStream[Infrastructure, Throwable, Byte], offerId: CarOffer.Id)(
      rawToken: String
  ): ZIO[Infrastructure, ApplicationError, Unit]

  def get(offerId: CarOffer.Id): IO[ApplicationError, CarOffer]

  def listByUser(rawToken: String): IO[ApplicationError, List[CarOffer]]

  def delete(offerId: CarOffer.Id)(rawToken: String): IO[ApplicationError, Unit]
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
  )(rawToken: String): ZIO[CarOffers, ApplicationError, CarOffer] =
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

  def addImage(img: ZStream[Infrastructure, Throwable, Byte], offerId: CarOffer.Id)(
      rawToken: String
  ): ZIO[Infrastructure, ApplicationError, Unit] =
    ZIO.serviceWithZIO[CarOffers](_.addImage(img, offerId)(rawToken))

  def get(offerId: CarOffer.Id): ZIO[CarOffers, ApplicationError, CarOffer] =
    ZIO.serviceWithZIO[CarOffers](_.get(offerId))

  def delete(offerId: CarOffer.Id)(rawToken: String): ZIO[CarOffers, ApplicationError, Unit] =
    ZIO.serviceWithZIO[CarOffers](_.delete(offerId)(rawToken))

  def listByUser(rawToken: String): ZIO[CarOffers, ApplicationError, List[CarOffer]] =
    ZIO.serviceWithZIO(_.listByUser(rawToken))

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
    )(rawToken: String): IO[ApplicationError, CarOffer] =
      for {
        _ <- ZIO.logInfo("Add car offer request")
        user <- getUser(rawToken)(tokens, usersRepository)
        _ <- user.validate
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
        offerO <- carOffersRepo.get(id) // ugly
        offer <- ZIO.fromOption(offerO).orElseFail(ApplicationError.OfferNotFound("ugly get offer"))
        _ <- ZIO.logInfo("Successfully added offer")
      } yield offer

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
        img: ZStream[Infrastructure, Throwable, Byte],
        offerId: CarOffer.Id
    )(rawToken: String): ZIO[Infrastructure, ApplicationError, Unit] =
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

    override def get(offerId: CarOffer.Id): IO[ApplicationError, CarOffer] =
      for {
        offerO <- carOffersRepo.get(offerId)
        offer <-
          ZIO.fromOption(offerO).orElseFail(ApplicationError.OfferNotFound(offerId.value.toString))
      } yield offer

    override def delete(offerId: CarOffer.Id)(rawToken: String): IO[ApplicationError, Unit] =
      for {
        _ <- ZIO.logInfo("Delete offer request")
        user <- getUser(rawToken)(tokens, usersRepository)
        offerO <- carOffersRepo.get(offerId)
        offer <-
          ZIO.fromOption(offerO).orElseFail(ApplicationError.OfferNotFound(offerId.value.toString))
        _ <- ZIO.unless(user.id == offer.ownerId)(ZIO.fail(ApplicationError.NotAnOwner))
        _ <- carOffersRepo.delete(offerId)
        _ <-
          s3.deleteObject(cfg.bucketName, s"offers/${offerId.value}")
            .mapError(err => UnexpectedError(err.getMessage): ApplicationError)
            .tapError(err => ZIO.logError(s"Failed to delete offer images: $err"))
        _ <- ZIO.logInfo("Successfully deleted offer")
      } yield ()

    override def listByUser(rawToken: String): IO[ApplicationError, List[CarOffer]] =
      for {
        _ <- ZIO.logInfo("List offers by user request")
        user <- getUser(rawToken)(tokens, usersRepository)
        offers <- carOffersRepo.listByOwner(user.id)
      } yield offers
  }

  val live = ZLayer.fromFunction(CarOffersLive.apply _)
}
