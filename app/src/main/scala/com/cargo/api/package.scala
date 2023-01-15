package com.cargo

import com.cargo.api.generated.definitions.dto.{
  CarOfferRes,
  Feature,
  ImageUrl,
  Point,
  UserInfo,
  Reservation => ReservationDTO
}
import io.scalaland.chimney.dsl._
import cats.syntax.option._
import com.cargo.algebra.{Authentication, CarOffers, Reservations, UserManager}
import com.cargo.model.{CarOffer, Reservation, User}

import java.time.{LocalDate, ZoneOffset}
import java.util.UUID
import scala.util.Try

package object api {
  type Infrastructure = Authentication with CarOffers with Reservations with UserManager

  def parseToken(bearerToken: String): String = bearerToken.drop(7)

  def mapReservations: PartialFunction[(Reservation, String, String), ReservationDTO] = {
    case (reservation, make, model) =>
      reservation
        .into[ReservationDTO]
        .withFieldComputed(_.from, r => LocalDate.ofInstant(r.startDate, ZoneOffset.UTC))
        .withFieldComputed(_.to, r => LocalDate.ofInstant(r.endDate, ZoneOffset.UTC))
        .withFieldComputed(_.renterId, _.renterId.value.toString)
        .withFieldComputed(_.make, _ => make)
        .withFieldComputed(_.model, _ => model)
        .transform
  }

  def mapCarOffer: PartialFunction[CarOffer, CarOfferRes] =
    _.into[CarOfferRes]
      .withFieldComputed(_.id, _.id.value.toString)
      .withFieldComputed(_.ownerId, _.ownerId.value.toString)
      .withFieldComputed(_.features, _.features.flatMap(Feature.from).toVector)
      .withFieldComputed(
        _.point,
        o => o.geolocation.map(p => Point(p.lat.toString, p.lon.toString))
      )
      .withFieldComputed(_.images, _.imageUrls.map(url => ImageUrl(url.some)).toVector)
      .transform

  def mapUser: PartialFunction[User, UserInfo] =
    _.into[UserInfo]
      .withFieldComputed(_.id, _.id.value.toString)
      .transform

  def parseUUID(str: String): Option[UUID] = Try(UUID.fromString(str)).toOption
}
