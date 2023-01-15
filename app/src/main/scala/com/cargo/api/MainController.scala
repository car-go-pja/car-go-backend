package com.cargo.api

import com.cargo.algebra.{Authentication, CarOffers, Reservations, UserManager}
import cats.syntax.all._
import zio._
import com.cargo.api.generated.{Handler, Resource}
import com.cargo.api.generated.definitions.dto._
import com.cargo.model.{CarOffer, Point}
import zio.stream.interop.fs2z._
import com.cargo.error.ApplicationError
import fs2.Stream

import java.time.{LocalDate, ZoneOffset}

final class MainController extends Handler[RIO[Infrastructure, *]] {
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
      .map(mapUser)
      .map(respond.Ok)
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
          .catchAll(err =>
            catchBadRequestError(
              respond.BadRequest,
              x => catchApplicationError(respond.Unauthorized)(x)
            )(err)
          )
      case None => ZIO.succeed(respond.BadRequest(ErrorResponse("invalid_body")))
    }

  override def getOffers(respond: Resource.GetOffersResponse.type)(
      from: Option[LocalDate],
      to: Option[LocalDate],
      city: Option[String],
      features: Option[String]
  ): RIO[CarOffers, Resource.GetOffersResponse] =
    CarOffers
      .list(
        from = from.map(_.atStartOfDay().toInstant(ZoneOffset.UTC)),
        to = to.map(_.atStartOfDay().toInstant(ZoneOffset.UTC)),
        city,
        features = features.fold(List.empty[String])(_.split(',').toList)
      )
      .map(offers => respond.Ok(offers.map(mapCarOffer).toVector))
      .catchAll(err => ZIO.fail(new RuntimeException(err.toString)))

  override def postUserProfile(respond: Resource.PostUserProfileResponse.type)(
      body: Option[UserProfile],
      authorization: String
  ): RIO[UserManager, Resource.PostUserProfileResponse] =
    body match {
      case Some(UserProfile(firstName, lastName, phone, dob, drivingLicence)) =>
        UserManager
          .updateProfile(firstName, lastName, phone, dob, drivingLicence)(parseToken(authorization))
          .as(respond.Created)
          .catchAll(err => catchApplicationError(respond.Unauthorized)(err))
      case _ => ZIO.succeed(respond.Forbidden(ErrorResponse("invalid_body", None)))
    }

  override def addPictures(respond: Resource.AddPicturesResponse.type)(
      offerId: String,
      image: Stream[RIO[Infrastructure, *], Byte],
      authorization: String
  ): RIO[Infrastructure, Resource.AddPicturesResponse] =
    parseUUID(offerId) match {
      case Some(uuid) =>
        CarOffers
          .addImage(image.toZStream(), CarOffer.Id(uuid))(
            parseToken(authorization)
          )
          .as(respond.Created)
          .catchAll(err =>
            catchBadRequestError(
              respond.BadRequest,
              x => catchApplicationError(respond.Unauthorized)(x)
            )(err)
          )
      case _ => ZIO.succeed(respond.BadRequest(ErrorResponse("invalid_offer_id", None)))
    }

  override def getCarOffer(respond: Resource.GetCarOfferResponse.type)(
      offerId: String
  ): RIO[Authentication with CarOffers, Resource.GetCarOfferResponse] =
    parseUUID(offerId) match {
      case Some(uuid) =>
        CarOffers
          .get(CarOffer.Id(uuid))
          .map(mapCarOffer)
          .map(respond.Ok)
          .catchAll(err => catchApplicationError(respond.BadRequest)(err))
      case _ => ZIO.succeed(respond.BadRequest(ErrorResponse("invalid_offer_id", None)))
    }

  override def deleteOffer(respond: Resource.DeleteOfferResponse.type)(
      offerId: String,
      authorization: String
  ): RIO[Authentication with CarOffers, Resource.DeleteOfferResponse] =
    parseUUID(offerId) match {
      case Some(uuid) =>
        CarOffers
          .delete(CarOffer.Id(uuid))(parseToken(authorization))
          .as(respond.NoContent)
          .catchAll(err =>
            catchBadRequestError(
              respond.NotFound,
              x => catchApplicationError(respond.Unauthorized)(x)
            )(err)
          )
      case _ => ZIO.succeed(respond.BadRequest(ErrorResponse("invalid_offer_id", None)))
    }

  override def makeReservation(respond: Resource.MakeReservationResponse.type)(
      body: Option[MakeReservation],
      authorization: String
  ): RIO[Reservations, Resource.MakeReservationResponse] =
    body match {
      case Some(MakeReservation(from, to, insurance, offerId)) =>
        Reservations
          .makeReservation(
            CarOffer.Id(parseUUID(offerId).get),
            from.atStartOfDay().toInstant(ZoneOffset.UTC),
            to.atStartOfDay().toInstant(ZoneOffset.UTC),
            insurance
          )(parseToken(authorization))
          .as(respond.NoContent)
          .catchAll {
            case _: ApplicationError.CarUnavailable.type =>
              ZIO.succeed(respond.BadRequest(ErrorResponse("car_unavailable", None)))
            case _: ApplicationError.InsufficientBalance.type =>
              ZIO.succeed(respond.BadRequest(ErrorResponse("insufficient_balance", None)))
            case _: ApplicationError.OwnerSelfRent.type =>
              ZIO.succeed(respond.Forbidden(ErrorResponse("owner_self_rent", None)))
            case ApplicationError.MissingInfo(msg) =>
              ZIO.succeed(respond.BadRequest(ErrorResponse("missing_info", msg.some)))
            case other => catchApplicationError(respond.Unauthorized)(other)
          }
      case None => ZIO.succeed(respond.BadRequest(ErrorResponse("invalid_body", None)))
    }

  override def getOwnersReservations(respond: Resource.GetOwnersReservationsResponse.type)(
      authorization: String
  ): RIO[Infrastructure, Resource.GetOwnersReservationsResponse] =
    Reservations
      .list(parseToken(authorization))
      .map(_.map(mapReservations).toVector)
      .map(respond.Ok)
      .catchAll(err => catchApplicationError(respond.Unauthorized)(err))

  override def addBalance(respond: Resource.AddBalanceResponse.type)(
      amount: BigDecimal,
      authorization: String
  ): RIO[UserManager, Resource.AddBalanceResponse] =
    UserManager
      .addBalance(amount)(parseToken(authorization))
      .as(respond.Ok)
      .catchAll(err => catchApplicationError(respond.Unauthorized)(err))

  override def verifyResetPassword(respond: Resource.VerifyResetPasswordResponse.type)(
      body: Option[ResetPassword],
      authorization: String
  ): RIO[Infrastructure, Resource.VerifyResetPasswordResponse] = ???

  override def getUserOffers(respond: Resource.GetUserOffersResponse.type)(
      authorization: String
  ): RIO[CarOffers, Resource.GetUserOffersResponse] =
    CarOffers
      .listByUser(parseToken(authorization))
      .map(offers => respond.Ok(offers.map(mapCarOffer).toVector))
      .catchAll(err => catchApplicationError(respond.Unauthorized)(err))

  override def resetPassword(respond: Resource.ResetPasswordResponse.type)(
      email: String
  ): RIO[Infrastructure, Resource.ResetPasswordResponse] = ???

  override def chooseReservation(respond: Resource.ChooseReservationResponse.type)(
      reservationId: String,
      body: Option[ReservationDecision],
      authorization: Option[String]
  ): RIO[Infrastructure, Resource.ChooseReservationResponse] = ???

  private def catchBadRequestError[Resp](
      badRequest: ErrorResponse => Resp,
      orElse: ApplicationError => Task[Resp]
  )(error: ApplicationError): ZIO[Any, Throwable, Resp] =
    error match {
      case ApplicationError.OfferNotFound(msg) =>
        ZIO.succeed(badRequest(ErrorResponse("offer_not_found", msg.some)))
      case _: ApplicationError.UserNotFound.type =>
        ZIO.succeed(badRequest(ErrorResponse("user_not_found", None)))
      case ApplicationError.MissingInfo(msg) =>
        ZIO.succeed(badRequest(ErrorResponse("missing_info", msg.some)))
      case other => orElse(other)
    }

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
      case ApplicationError.MissingInfo(msg) =>
        ZIO.fail(new RuntimeException(s"missing info error: $msg"))
      case _: ApplicationError.InvalidCode.type =>
        ZIO.succeed(unauthorized(ErrorResponse("invalid_code", None)))
      case _: ApplicationError.CarUnavailable.type =>
        ZIO.succeed(unauthorized(ErrorResponse("car_unavailable", None)))
      case _: ApplicationError.InsufficientBalance.type =>
        ZIO.succeed(unauthorized(ErrorResponse("insufficient_balance", None)))
      case _: ApplicationError.OwnerSelfRent.type =>
        ZIO.succeed(unauthorized(ErrorResponse("owner_self_rent", None)))
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
