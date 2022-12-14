package com.cargo.repository

import com.cargo.model.{User, VerificationRow}
import doobie._
import doobie.implicits._
import doobie.postgres.implicits._
import zio.interop.catz._
import com.cargo.error.ApplicationError._
import zio._
import java.time.Instant
import java.util.UUID

import cats.effect.MonadCancelThrow

trait UsersRepository {
  def create(id: UUID, email: String, password: String): IO[DatabaseError.type, Unit]
  def find(email: String): IO[DatabaseError.type, Option[User]]
  def saveVerificationCode(
      id: UUID,
      code: String,
      userId: UUID,
      createdAt: Instant
  ): IO[DatabaseError.type, Unit]
  def markAsVerified(userId: UUID): IO[DatabaseError.type, Unit]
  def isVerified(userId: UUID): IO[DatabaseError.type, Boolean]
  def findVerificationRow(userId: UUID): IO[DatabaseError.type, Option[VerificationRow]]
}

object UsersRepository {
  final case class UsersLive(xa: Transactor[Task]) extends UsersRepository {
    override def create(id: UUID, email: String, password: String): IO[DatabaseError.type, Unit] =
      SQL
        .create(id, email, password)
        .run
        .transact(xa)
        .unit
        .mapError(_ => DatabaseError)

    override def find(email: String): IO[DatabaseError.type, Option[User]] =
      SQL.find(email).option.transact(xa).orElseFail(DatabaseError)

    override def saveVerificationCode(
        id: UUID,
        code: String,
        userId: UUID,
        createdAt: Instant
    ): IO[DatabaseError.type, Unit] =
      SQL
        .saveVerificationCode(id, code, userId, createdAt)
        .run
        .transact(xa)
        .unit
        .orElseFail(DatabaseError)

    override def markAsVerified(userId: UUID): IO[DatabaseError.type, Unit] =
      SQL.markAsVerified(userId).run.transact(xa).unit.orElseFail(DatabaseError)

    override def isVerified(userId: UUID): IO[DatabaseError.type, Boolean] =
      SQL
        .isVerified(userId)
        .option
        .transact(xa)
        .map(_.getOrElse(false))
        .orElseFail(DatabaseError) // fixme return error in io if user doesn't exist

    override def findVerificationRow(
        userId: UUID
    ): IO[DatabaseError.type, Option[VerificationRow]] =
      SQL.findVerificationRow(userId).option.transact(xa).orElseFail(DatabaseError)
  }

  val live = ZLayer.fromFunction(UsersLive.apply _)

  private object SQL {
    def create(id: UUID, email: String, password: String): Update0 =
      sql"""INSERT INTO cargo.users (id, email, password, is_verified) VALUES ($id, $email, $password, false)""".update

    def find(email: String): Query0[User] =
      sql"""SELECT * FROM cargo.users WHERE email = $email""".query[User]

    def saveVerificationCode(id: UUID, code: String, userId: UUID, createdAt: Instant): Update0 =
      sql"""INSERT INTO cargo.verification (id, user_id, code, created_at) VALUES ($id, $userId, $code, $createdAt)""".update

    def markAsVerified(userId: UUID): Update0 =
      sql"""UPDATE cargo.users SET is_verified = true WHERE id = $userId""".update

    def isVerified(userId: UUID): Query0[Boolean] =
      sql"""SELECT is_verified FROM cargo.users WHERE id = $userId""".query[Boolean]

    def findVerificationRow(userId: UUID): Query0[VerificationRow] =
      sql"""SELECT * FROM cargo.verification WHERE user_id = $userId""".query[VerificationRow]
  }
}
