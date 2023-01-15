package com.cargo.repository

import com.cargo.error.ApplicationError
import com.cargo.error.ApplicationError.DatabaseError
import com.cargo.infrastructure.DoobieInstances
import com.cargo.model.{CarOffer, User, Point}
import doobie._
import doobie.implicits._
import Fragments.whereAndOpt
import doobie.postgres.implicits._
import org.postgresql.geometric.PGpoint
import zio.interop.catz._

import java.time.Instant
import zio._

trait CarOffersRepository {
  def create(
      id: CarOffer.Id,
      ownerId: User.Id,
      make: String,
      model: String,
      year: String,
      pricePerDay: BigDecimal,
      horsepower: String,
      fuelType: String,
      features: List[String],
      city: String,
      seatsAmount: String,
      geolocation: Option[Point],
      createdAt: Instant
  ): IO[DatabaseError.type, Unit]

  def list(
      from: Option[Instant],
      to: Option[Instant],
      city: Option[String],
      features: List[String]
  ): IO[DatabaseError.type, List[CarOffer]]

  def get(offerId: CarOffer.Id): IO[DatabaseError.type, Option[CarOffer]]

  def saveImage(url: String, offerId: CarOffer.Id): IO[DatabaseError.type, Unit]

  def delete(offerId: CarOffer.Id): IO[DatabaseError.type, Unit]

  def listByOwner(ownerId: User.Id): IO[DatabaseError.type, List[CarOffer]]
}

object CarOffersRepository extends DoobieInstances {
  final case class CarOffersLive(xa: Transactor[Task]) extends CarOffersRepository {
    override def create(
        id: CarOffer.Id,
        ownerId: User.Id,
        make: String,
        model: String,
        year: String,
        pricePerDay: BigDecimal,
        horsepower: String,
        fuelType: String,
        features: List[String],
        city: String,
        seatsAmount: String,
        geolocation: Option[Point],
        createdAt: Instant
    ): IO[ApplicationError.DatabaseError.type, Unit] =
      SQL
        .create(
          id,
          ownerId,
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
          createdAt
        )
        .run
        .transact(xa)
        .unit
        .tapError(err => ZIO.logError(err.getMessage))
        .orElseFail(DatabaseError)

    override def list(
        from: Option[Instant],
        to: Option[Instant],
        city: Option[String],
        features: List[String]
    ): IO[ApplicationError.DatabaseError.type, List[CarOffer]] =
      SQL
        .list(from, to, city, features)
        .to[List]
        .transact(xa)
        .tapError(err => ZIO.logError(err.getMessage))
        .orElseFail(DatabaseError)

    override def get(
        offerId: CarOffer.Id
    ): IO[ApplicationError.DatabaseError.type, Option[CarOffer]] =
      SQL.get(offerId).option.transact(xa).orElseFail(DatabaseError)

    override def saveImage(
        url: String,
        offerId: CarOffer.Id
    ): IO[ApplicationError.DatabaseError.type, Unit] =
      SQL.saveImage(url, offerId).run.transact(xa).unit.orElseFail(DatabaseError)

    override def delete(offerId: CarOffer.Id): IO[ApplicationError.DatabaseError.type, Unit] =
      SQL.delete(offerId).run.transact(xa).unit.orElseFail(DatabaseError)

    override def listByOwner(ownerId: User.Id): IO[ApplicationError.DatabaseError.type, List[CarOffer]] =
      SQL.listByOwner(ownerId).to[List].transact(xa).orElseFail(DatabaseError)
  }

  private object SQL {
    def create(
        id: CarOffer.Id,
        ownerId: User.Id,
        make: String,
        model: String,
        year: String,
        pricePerDay: BigDecimal,
        horsepower: String,
        fuelType: String,
        features: List[String],
        city: String,
        seatsAmount: String,
        geolocation: Option[Point],
        createdAt: Instant
    ): Update0 =
      sql"""INSERT INTO cargo.car_offers (id, owner_id, make, model, year, price_per_day, horsepower, fuel_type, features, city, seats_amount, geolocation, created_at, img_urls)
           VALUES ($id, $ownerId, $make, $model, $year, $pricePerDay, $horsepower, $fuelType, $features, $city, $seatsAmount, $geolocation, $createdAt, '{}')""".update

    def list(
        from: Option[Instant],
        to: Option[Instant],
        city: Option[String],
        features: List[String]
    ): Query0[CarOffer] = {
      val isCity = city.map(c => fr"city = $c")
      val containsFeatures = Option.unless(features.isEmpty)(
        fr"EXISTS (SELECT 1 FROM unnest($features) WHERE unnest = ANY(features) AND features @> $features)"
      )

      (fr"SELECT * FROM cargo.car_offers" ++ whereAndOpt(isCity, containsFeatures))
        .query[CarOffer]
    }

    def listByOwner(ownerId: User.Id): Query0[CarOffer] =
      sql"SELECT * FROM cargo.car_offers WHERE owner_id = $ownerId".query[CarOffer]

    def get(offerId: CarOffer.Id): Query0[CarOffer] =
      sql"SELECT * FROM cargo.car_offers WHERE id = $offerId".query[CarOffer]

    def saveImage(url: String, offerId: CarOffer.Id): Update0 =
      sql"""UPDATE cargo.car_offers SET img_urls = array_append(img_urls, $url) WHERE id = $offerId""".update

    def delete(offerId: CarOffer.Id): Update0 =
      sql"DELETE FROM cargo.car_offers WHERE id = $offerId".update

    implicit val pointType: Meta[Point] =
      Meta[PGpoint].timap(p => Point(p.x, p.y))(p => new PGpoint(p.lat, p.lon))
  }

  val live = ZLayer.fromFunction(CarOffersLive.apply _)
}
