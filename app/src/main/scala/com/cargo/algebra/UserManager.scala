package com.cargo.algebra

import com.cargo.error.ApplicationError
import com.cargo.model.User
import com.cargo.repository.UsersRepository
import com.password4j.Password
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

  def setNewPassword(password: String, resetToken: String): IO[ApplicationError, Unit]

  def getUserById(userId: User.Id)(rawToken: String): IO[ApplicationError, User]
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

  def setNewPassword(
      password: String,
      resetToken: String
  ): ZIO[UserManager, ApplicationError, Unit] =
    ZIO.serviceWithZIO(_.setNewPassword(password, resetToken))

  def getUserById(userId: User.Id)(rawToken: String): ZIO[UserManager, ApplicationError, User] =
    ZIO.serviceWithZIO(_.getUserById(userId)(rawToken))

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

    override def setNewPassword(password: String, resetToken: String): IO[ApplicationError, Unit] =
      usersRepo.setNewPassword(Password.hash(password).withBcrypt.getResult, resetToken)

    override def getUserById(userId: User.Id)(rawToken: String): IO[ApplicationError, User] =
      for {
        _ <- getUser(rawToken)(tokens, usersRepo)
        user <- usersRepo.findById(userId)
      } yield user
  }

  val live = ZLayer.fromFunction(UserManagerLive.apply _)
}
