package com.cargo.api

import com.cargo.algebra.{Authentication, CarOffers}
import cats.syntax.option._
import zio._
import com.cargo.api.generated.{Handler, Resource}
import com.cargo.api.generated.definitions.dto._
import com.cargo.model.{CarOffer, Point}
import zio.stream.interop.fs2z._
import com.cargo.error.ApplicationError

import java.time.Instant
import java.util.UUID

final class MainController extends Handler[RIO[Authentication with CarOffers, *]] {
  override def login(
      respond: Resource.LoginResponse.type
  )(body: Option[UserCredentials]): RIO[Authentication, Resource.LoginResponse] =
    body match {
      case Some(credentials) =>
        Authentication
          .login(credentials.email, credentials.password)
          .map(token => respond.Ok(AccessToken(token.encodedToken)))
          .catchAll(err => catchApplicationError(respond.Unauthorized)(err))
      case None => ZIO.succeed(respond.Unauthorized(ErrorResponse("invalid_credentials", None)))
    }

  override def registerUser(respond: Resource.RegisterUserResponse.type)(
      body: Option[UserCredentials]
  ): RIO[Authentication, Resource.RegisterUserResponse] =
    body match {
      case Some(credentials) =>
        Authentication
          .registerUser(credentials.email, credentials.password)
          .map(token => respond.Ok(VerificationToken(token.encodedToken.some)))
          .catchAll(err => catchApplicationError(respond.Unauthorized)(err)) //fixme
      case None => ZIO.succeed(respond.BadRequest(ErrorResponse("invalid_body")))
    }

  override def verifyEmail(
      respond: Resource.VerifyEmailResponse.type
  )(code: String, authorization: String): RIO[Authentication, Resource.VerifyEmailResponse] =
    Authentication
      .verifyEmail(code)(parseToken(authorization)) //fixme parse bearer token (middleware?)
      .as(respond.NoContent)
      .catchAll(err => catchApplicationError(respond.Unauthorized)(err))

  override def getUser(respond: Resource.GetUserResponse.type)(
      authorization: String
  ): RIO[Authentication, Resource.GetUserResponse] =
    Authentication
      .getUserInfo(parseToken(authorization))
      .map(user => respond.Ok(UserInfo(user.id.toString, user.email, user.isVerified)))
      .catchAll(err => catchApplicationError(respond.Unauthorized)(err))

  override def postOffersAdd(respond: Resource.PostOffersAddResponse.type)(
      body: Option[CarOfferReq],
      authorization: String
  ): RIO[CarOffers, Resource.PostOffersAddResponse] =
    body match {
      case Some(req) =>
        CarOffers
          .add(
            make = req.make,
            model = req.model,
            year = req.year,
            pricePerDay = BigDecimal(req.pricePerDay),
            horsepower = req.horsepower,
            fuelType = req.fuelType.toString,
            features = req.features.map(_.value).toList,
            city = req.city,
            seatsAmount = req.seatsAmount,
            geolocation = req.point.map(p => Point(p.lat.toDouble, p.lon.toDouble))
          )(parseToken(authorization))
          .map(mapCarOffer)
          .map(respond.Ok)
          .catchAll(err => catchApplicationError(respond.Unauthorized)(err))
      case None => ZIO.succeed(respond.BadRequest(ErrorResponse("invalid_body")))
    }

  override def getOffers(respond: Resource.GetOffersResponse.type)(
      from: Option[String],
      to: Option[String],
      city: Option[String],
      features: Option[String]
  ): RIO[CarOffers, Resource.GetOffersResponse] =
    CarOffers
      .list(
        from = from.map(s => Instant.parse(s)),
        to = to.map(s => Instant.parse(s)),
        city,
        features = features.fold(List.empty[String])(_.split(',').toList)
      )
      .map(offers => respond.Ok(offers.map(mapCarOffer).toVector))
      .catchAll(err => ZIO.fail(new RuntimeException(err.toString)))

  override def postUserProfile(respond: Resource.PostUserProfileResponse.type)(
      body: Option[UserProfile],
      authorization: String
  ): RIO[CarOffers, Resource.PostUserProfileResponse] = ???

  override def postOfferOfferId(respond: Resource.PostOfferOfferIdResponse.type)(
      offerId: String,
      image: fs2.Stream[RIO[Authentication with CarOffers, *], Byte],
      authorization: String
  ): RIO[Authentication with CarOffers, Resource.PostOfferOfferIdResponse] =
    CarOffers
      .addImage(image.toZStream(), CarOffer.Id(UUID.fromString(offerId)))(parseToken(authorization))
      .as(respond.Created) //add bad request for uuid and forbidden for not an owner
      .catchAll(err => catchApplicationError(respond.Unauthorized)(err))

  override def postReservationOfferId(respond: Resource.PostReservationOfferIdResponse.type)(
      offerId: String,
      body: Option[MakeReservation],
      authorization: String
  ): RIO[Authentication with CarOffers, Resource.PostReservationOfferIdResponse] = ???

  override def deleteOffer(respond: Resource.DeleteOfferResponse.type)(
      offerId: String,
      authorization: String
  ): RIO[Authentication with CarOffers, Resource.DeleteOfferResponse] = ???

  override def getReservation(respond: Resource.GetReservationResponse.type)(
      offerId: String,
      body: Option[Vector[Reservation]]
  ): RIO[Authentication with CarOffers, Resource.GetReservationResponse] = ???

  private def catchBadRequestError[Resp](
      badRequest: ErrorResponse => Resp,
      orElse: ApplicationError => Resp
  )(error: ApplicationError): ZIO[Any, Throwable, Resp] = ???

  private def catchApplicationError[Resp](
      unauthorized: ErrorResponse => Resp
  )(error: ApplicationError): ZIO[Any, Throwable, Resp] =
    error match {
      case _: ApplicationError.DatabaseError.type =>
        ZIO.fail(new RuntimeException("database exception"))
      case ApplicationError.IntegrationError(msg) =>
        ZIO.fail(new RuntimeException(s"integration failure $msg")) //fixme 502 error
      case ApplicationError.UnexpectedError(msg) =>
        ZIO.fail(new RuntimeException(s"unexpected error: $msg"))
      case _: ApplicationError.InvalidCode.type =>
        ZIO.succeed(unauthorized(ErrorResponse("invalid_code", None)))
      case ApplicationError.OfferNotFound(msg) =>
        ZIO.succeed(unauthorized(ErrorResponse("offer_not_found", msg.some)))
      case _: ApplicationError.NotAnOwner.type =>
        ZIO.succeed(unauthorized(ErrorResponse("forbidden", None)))
      case _: ApplicationError.InvalidPassword.type =>
        ZIO.succeed(unauthorized(ErrorResponse("invalid_password", None)))
      case ApplicationError.InvalidToken(msg) =>
        ZIO.succeed(unauthorized(ErrorResponse("invalid_token", msg.some)))
      case ApplicationError.UserNotFound =>
        ZIO.succeed(unauthorized(ErrorResponse("user_not_found", None)))
    }
}
