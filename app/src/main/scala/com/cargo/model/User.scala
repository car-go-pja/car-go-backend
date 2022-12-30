package com.cargo.model

import io.estatico.newtype.macros.newtype

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
    dob: Option[LocalDate]
)

object User {
  @newtype final case class Id(value: UUID)
}
