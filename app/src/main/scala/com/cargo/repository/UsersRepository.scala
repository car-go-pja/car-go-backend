package com.cargo.repository

import com.cargo.model.User
import doobie._
import doobie.implicits._
import doobie.postgres.implicits._
import zio.interop.catz._
import zio._

import java.util.UUID

trait UsersRepository {
  def create(id: UUID, email: String, password: String): Task[Unit]
  def find(email: String): Task[Option[User]]
}

object UsersRepository {
  final case class UsersLive(xa: Transactor[Task]) extends UsersRepository {
    override def create(id: UUID, email: String, password: String): Task[Unit] =
      SQL.create(id, email, password).run.transact(xa).unit

    override def find(email: String): Task[Option[User]] =
      SQL.find(email).option.transact(xa)
  }

  val live = ZLayer.fromFunction(UsersLive.apply _)

  private object SQL {
    def create(id: UUID, email: String, password: String): Update0 =
      sql"""INSERT INTO cargo.users (id, email, password) VALUES ($id, $email, $password)""".update

    def find(email: String): Query0[User] =
      sql"""SELECT * FROM cargo.users WHERE email = $email""".query[User]
  }
}
