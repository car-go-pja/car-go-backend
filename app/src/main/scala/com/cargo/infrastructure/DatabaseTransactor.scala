package com.cargo.infrastructure

import com.cargo.config.DatabaseConfig
import doobie.Transactor
import doobie.hikari.HikariTransactor
import zio.interop.catz._
import zio.{Task, ZIO, ZLayer}

object DatabaseTransactor {

  val live: ZLayer[DatabaseConfig, Throwable, Transactor[Task]] = ZLayer.scoped {
    for {
      cfg <- ZIO.service[DatabaseConfig]
      executionContext <- ZIO.blockingExecutor.map(_.asExecutionContext)
      transactor <-
        HikariTransactor
          .newHikariTransactor[Task](
            "org.postgresql.Driver",
            cfg.jdbcUrl,
            cfg.username,
            cfg.password,
            executionContext
          )
          .toScopedZIO
    } yield transactor
  }
}
