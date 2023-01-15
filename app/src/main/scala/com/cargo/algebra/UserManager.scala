package com.cargo.algebra

import com.cargo.error.ApplicationError
import com.cargo.repository.UsersRepository
import zio._

import java.time.LocalDate

trait UserManager {
  def updateProfile(
      firstName: Option[String],
      lastName: Option[String],
      phone: Option[String],
      dob: Option[LocalDate],
      drivingLicence: Option[String]
  )(rawToken: String): IO[ApplicationError, Unit]

  def addBalance(amount: BigDecimal)(rawToken: String): IO[ApplicationError, Unit]
}

object UserManager {

  def addBalance(amount: BigDecimal)(rawToken: String): ZIO[UserManager, ApplicationError, Unit] =
    ZIO.serviceWithZIO(_.addBalance(amount)(rawToken))

  def updateProfile(
      firstName: Option[String],
      lastName: Option[String],
      phone: Option[String],
      dob: Option[LocalDate],
      drivingLicence: Option[String]
  )(rawToken: String): ZIO[UserManager, ApplicationError, Unit] =
    ZIO.serviceWithZIO(_.updateProfile(firstName, lastName, phone, dob, drivingLicence)(rawToken))

  final case class UserManagerLive(usersRepo: UsersRepository, tokens: Tokens) extends UserManager {
    override def updateProfile(
        firstName: Option[String],
        lastName: Option[String],
        phone: Option[String],
        dob: Option[LocalDate],
        drivingLicence: Option[String]
    )(rawToken: String): IO[ApplicationError, Unit] =
      for {
        _ <- ZIO.logInfo("Update profile request")
        user <- getUser(rawToken)(tokens, usersRepo)
        _ <- usersRepo.updateProfile(user.id, firstName, lastName, phone, dob, drivingLicence)
        _ <- ZIO.logInfo("Successfully updated profile")
      } yield ()

    override def addBalance(amount: BigDecimal)(rawToken: String): IO[ApplicationError, Unit] =
      for {
        user <- getUser(rawToken)(tokens, usersRepo)
        _ <- usersRepo.changeBalance(user.id, amount)
      } yield ()
  }

  val live = ZLayer.fromFunction(UserManagerLive.apply _)
}
