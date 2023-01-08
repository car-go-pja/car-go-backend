package com.cargo.model

import io.estatico.newtype.macros.newtype

import java.time.Instant
import java.util.UUID

final case class CarOffer(
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
    createdAt: Instant,
    imageUrls: List[String]
)

object CarOffer {
  @newtype final case class Id(value: UUID)
}
