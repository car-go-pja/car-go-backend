package com.cargo.model

import doobie.util.meta.Meta
import io.estatico.newtype.macros.newtype

import java.util.UUID
import java.time.Instant

final case class Reservation(
    id: Reservation.Id,
    renterId: User.Id,
    offerId: User.Id,
    status: Reservation.Status,
    startDate: Instant,
    endDate: Instant,
    totalPrice: BigDecimal,
    createdAt: Instant
)

object Reservation {
  @newtype final case class Id(value: UUID)

  sealed abstract class Status(val name: String)

  object Status {
    case object Requested extends Status("requested")
    case object Accepted extends Status("accepted")
    case object Denied extends Status("denied")

    def fromString(str: String): Option[Status] =
      str match {
        case Requested.name  => Some(Requested)
        case Accepted.name   => Some(Accepted)
        case Denied.name     => Some(Denied)
        case _               => None
      }

    implicit val statusMeta: Meta[Status] = Meta[String].imap(Status.fromString(_).orNull)(_.name)
  }
}
