package com.cargo.model

import cats.data.{Validated, ValidatedNec}
import cats.data.Validated.Valid
import cats.syntax.all._
import com.cargo.error.ApplicationError
import com.cargo.error.ApplicationError.MissingInfo
import io.estatico.newtype.macros.newtype
import zio._

import java.time.LocalDate
import java.util.UUID

final case class User(
    id: User.Id,
    email: String,
    password: String,
    isVerified: Boolean,
    firstName: Option[String],
    lastName: Option[String],
    drivingLicence: Option[String],
    phone: Option[String],
    balance: BigDecimal,
    dob: Option[LocalDate],
    resetToken: Option[String]
) {
  def validate: IO[MissingInfo, User] =
    ZIO
      .fromEither(
        Validated
          .fromOption(firstName, MissingInfo("first name"))
          .andThen(_ => Validated.fromOption(lastName, MissingInfo("last name")))
          .andThen(_ => Validated.fromOption(drivingLicence, MissingInfo("driving licence")))
          .andThen(_ => Validated.fromOption(phone, MissingInfo("phone")))
          .andThen(_ => Validated.fromOption(dob, MissingInfo("date of birth")))
          .andThen(_ => Valid(this))
          .toValidatedNec
          .toEither
      )
      .mapError(l => MissingInfo(l.map(_.msg).toList.mkString(", ")))
      .tapError(err => ZIO.logInfo(s"Validation error ${err.msg}"))
}

object User {
  @newtype final case class Id(value: UUID)
}
