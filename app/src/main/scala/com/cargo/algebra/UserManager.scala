package com.cargo.algebra

import com.cargo.error.ApplicationError
import com.cargo.repository.UsersRepository
import zio._

trait UserManager {
  def updateProfile(): IO[ApplicationError, Unit]
  def addBalance(amount: BigDecimal)(rawToken: String): IO[ApplicationError, Unit]
}

object UserManager {

  def addBalance(amount: BigDecimal)(rawToken: String): ZIO[UserManager, ApplicationError, Unit] =
    ZIO.serviceWithZIO(_.addBalance(amount)(rawToken))

  final case class UserManagerLive(usersRepo: UsersRepository, tokens: Tokens) extends UserManager {
    override def updateProfile(): IO[ApplicationError, Unit] = ???

    override def addBalance(amount: BigDecimal)(rawToken: String): IO[ApplicationError, Unit] =
      for {
        user <- getUser(rawToken)(tokens, usersRepo)
        _ <- usersRepo.changeBalance(user.id, amount)
      } yield ()
  }

  val live = ZLayer.fromFunction(UserManagerLive.apply _)
}
