package com.cargo.algebra

import com.cargo.api.generated.definitions.dto.Insurance
import com.cargo.error.ApplicationError
import com.cargo.model.{CarOffer, Reservation, User}
import com.cargo.model.Reservation.Status.Accepted
import com.cargo.repository.{CarOffersRepository, ReservationsRepository, UsersRepository}
import cats.syntax.option._
import com.cargo.error.ApplicationError.{CarUnavailable, InsufficientBalance}
import zio._

import java.time.Instant
import java.util.UUID

trait Reservations {
  def makeReservation(offerId: CarOffer.Id, from: Instant, to: Instant, insurance: Insurance)(
      rawToken: String
  ): IO[ApplicationError, Unit]

  def list(rawToken: String): IO[ApplicationError, List[(Reservation, String, String)]]
}

object Reservations {

  def makeReservation(offerId: CarOffer.Id, from: Instant, to: Instant, insurance: Insurance)(
      rawToken: String
  ): ZIO[Reservations, ApplicationError, Unit] =
    ZIO.serviceWithZIO[Reservations](_.makeReservation(offerId, from, to, insurance)(rawToken))

  def list(rawToken: String): ZIO[Reservations, ApplicationError, List[(Reservation, String, String)]] =
    ZIO.serviceWithZIO[Reservations](_.list(rawToken))

  final case class ReservationsLive(
      tokens: Tokens,
      reservationsRepo: ReservationsRepository,
      usersRepo: UsersRepository,
      offersRepo: CarOffersRepository
  ) extends Reservations {
    override def makeReservation(
        offerId: CarOffer.Id,
        from: Instant,
        to: Instant,
        insurance: Insurance
    )(
        rawToken: String
    ): IO[ApplicationError, Unit] = {
      val insurancePrice = insurance match {
        case Insurance.Cheap     => 50.0
        case Insurance.Medium    => 100.0
        case Insurance.Expensive => 150.0
      }

      for {
        _ <- ZIO.logInfo("Make reservation request")
        user <- getUser(rawToken)(tokens, usersRepo)
        offerO <- offersRepo.get(offerId)
        offer <-
          ZIO.fromOption(offerO).orElseFail(ApplicationError.OfferNotFound(offerId.value.toString))
        _ <- ZIO.unless(user.id != offer.ownerId)(ZIO.fail(ApplicationError.OwnerSelfRent))
        reservations <- reservationsRepo.getReservations(offerId, Accepted.some, (from, to).some)
        _ <- ZIO.unless(reservations.isEmpty)(ZIO.fail(CarUnavailable))
        reservationId <- ZIO.succeed(Reservation.Id(UUID.randomUUID()))
        days = Duration.fromInterval(from, to).toDays
        totalPrice = offer.pricePerDay * days + insurancePrice
        _ <- ZIO.unless(user.balance >= totalPrice)(ZIO.fail(InsufficientBalance))
        _ <- usersRepo.changeBalance(user.id, -totalPrice)
        _ <- reservationsRepo.createReservation(
          reservationId,
          user.id,
          offerId,
          from,
          to,
          totalPrice,
          Instant.now()
        )
        _ <- ZIO.logInfo("Successfully made reservation")
      } yield ()
    }

    override def list(rawToken: String): IO[ApplicationError, List[(Reservation, String, String)]] =
      for {
        _ <- ZIO.logInfo("List reservations request")
        user <- getUser(rawToken)(tokens, usersRepo)
        reservations <- reservationsRepo.getOwnerReservations(user.id)
        _ <- ZIO.logInfo(s"Successfully listed reservations size: ${reservations.size}")
      } yield reservations
  }

  val live = ZLayer.fromFunction(ReservationsLive.apply _)
}
