package com.cargo.repository

import zio._
import com.cargo.error.ApplicationError
import com.cargo.error.ApplicationError.DatabaseError
import com.cargo.model.{CarOffer, Reservation, User}
import doobie._
import doobie.implicits._
import doobie.postgres.implicits._
import Fragments.whereAndOpt
import com.cargo.infrastructure.DoobieInstances
import zio.interop.catz._
import doobie.Transactor

import java.time.Instant

trait ReservationsRepository {
  def createReservation(
      id: Reservation.Id,
      renterId: User.Id,
      offerId: CarOffer.Id,
      startDate: Instant,
      endDate: Instant,
      totalPrice: BigDecimal,
      createdAt: Instant
  ): IO[DatabaseError.type, Unit]

  def getReservations(
      offerId: CarOffer.Id,
      status: Option[Reservation.Status],
      fromToDate: Option[(Instant, Instant)]
  ): IO[DatabaseError.type, List[Reservation]]

  def getOwnerReservations(
      ownerId: User.Id
  ): IO[DatabaseError.type, List[Reservation]]
}

object ReservationsRepository extends DoobieInstances {
  final case class ReservationsRepositoryLive(xa: Transactor[Task]) extends ReservationsRepository {
    override def createReservation(
        id: Reservation.Id,
        renterId: User.Id,
        offerId: CarOffer.Id,
        startDate: Instant,
        endDate: Instant,
        totalPrice: BigDecimal,
        createdAt: Instant
    ): IO[ApplicationError.DatabaseError.type, Unit] =
      SQL
        .createReservation(id, renterId, offerId, startDate, endDate, totalPrice, createdAt)
        .run
        .transact(xa)
        .unit
        .orElseFail(DatabaseError)

    override def getReservations(
        offerId: CarOffer.Id,
        status: Option[Reservation.Status],
        fromToDate: Option[(Instant, Instant)]
    ): IO[ApplicationError.DatabaseError.type, List[Reservation]] =
      SQL
        .getReservations(offerId, status, fromToDate)
        .to[List]
        .transact(xa)
        .orElseFail(DatabaseError)

    override def getOwnerReservations(
        ownerId: User.Id
    ): IO[ApplicationError.DatabaseError.type, List[Reservation]] =
      SQL
        .getOwnerReservations(ownerId)
        .to[List]
        .transact(xa)
        .mapError(x => DatabaseError)
  }

  private object SQL {
    def createReservation(
        id: Reservation.Id,
        renterId: User.Id,
        offerId: CarOffer.Id,
        startDate: Instant,
        endDate: Instant,
        totalPrice: BigDecimal,
        createdAt: Instant
    ): Update0 =
      sql"""INSERT INTO cargo.reservations (id, renter_id, offer_id, status, start_date, end_date, total_price, created_at) VALUES ($id, $renterId, $offerId, '${Reservation.Status.Requested}', $startDate, $endDate, $totalPrice, $createdAt)""".update

    def getReservations(
        offerId: CarOffer.Id,
        status: Option[Reservation.Status],
        fromToDate: Option[(Instant, Instant)]
    ): Query0[Reservation] = {
      val withStatus = status.map(s => fr"status = $s")
      val withinDates = fromToDate.map {
        case (from, to) => fr"(start_date, end_date) OVERLAPS ($from, $to)"
      }

      (fr"SELECT * FROM cargo.reservations" ++ whereAndOpt(
        withStatus,
        withinDates,
        Some(fr"offer_id = $offerId")
      )).query[Reservation]
    }

    def getOwnerReservations(ownerId: User.Id): Query0[Reservation] =
      sql"SELECT r.* FROM cargo.reservations r JOIN cargo.car_offers o ON r.offer_id = o.id WHERE o.owner_id = $ownerId"
        .query[Reservation]
  }

  val live = ZLayer.fromFunction(ReservationsRepositoryLive.apply _)
}
