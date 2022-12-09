package com.cargo.repository

import com.cargo.model.{User, VerificationRow}
import doobie._
import doobie.implicits._
import doobie.postgres.implicits._
import zio.interop.catz._
import zio._

import java.time.Instant
import java.util.UUID

trait UsersRepository {
  def create(id: UUID, email: String, password: String): Task[Unit]
  def find(email: String): Task[Option[User]]
  def saveVerificationCode(id: UUID, code: String, userId: UUID, createdAt: Instant): Task[Unit]
  def markAsVerified(userId: UUID): Task[Unit]
  def isVerified(userId: UUID): Task[Boolean]
  def findVerificationRow(userId: UUID): Task[Option[VerificationRow]]
}

object UsersRepository {
  final case class UsersLive(xa: Transactor[Task]) extends UsersRepository {
    override def create(id: UUID, email: String, password: String): Task[Unit] =
      SQL.create(id, email, password).run.transact(xa).unit

    override def find(email: String): Task[Option[User]] =
      SQL.find(email).option.transact(xa)

    override def saveVerificationCode(id: UUID, code: String, userId: UUID, createdAt: Instant): Task[Unit] =
      SQL.saveVerificationCode(id, code, userId, createdAt).run.transact(xa).unit

    override def markAsVerified(userId: UUID): Task[Unit] =
      SQL.markAsVerified(userId).run.transact(xa).unit

    override def isVerified(userId: UUID): Task[Boolean] =
      SQL.isVerified(userId).option.transact(xa).map(_.getOrElse(false)) // fixme return error in io if user doesn't exist

    override def findVerificationRow(userId: UUID): Task[Option[VerificationRow]] =
      SQL.findVerificationRow(userId).option.transact(xa)
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
