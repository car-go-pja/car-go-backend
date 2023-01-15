package com.cargo.algebra

import com.cargo.config.{DatabaseConfig, StorageConfig}
import zio.nio.file.Path
import zio.s3._
import com.cargo.error.ApplicationError
import com.cargo.service.Notifications
import org.postgresql.ds.PGSimpleDataSource
import io.zonky.test.db.postgres.embedded.{EmbeddedPostgres, FlywayPreparer}
import zio._

object TestUtils {
  val embeddedPostgresLayer: ZLayer[Any, Throwable, PGSimpleDataSource] = ZLayer.scoped {
    for {
      pg <- ZIO.acquireRelease {
        ZIO.attempt(EmbeddedPostgres.start)
      } { pg =>
        ZIO.logInfo("shutting down test db...") *>
          ZIO.attempt(pg.close).orDie
      }
      ds <- ZIO.attempt(pg.getPostgresDatabase.asInstanceOf[PGSimpleDataSource])
      _ <- ZIO.logInfo(s"Running db on ${ds.getUrl}")
      _ <- ZIO.attempt {
        val preparer = FlywayPreparer.forClasspathLocation("db/migration")
        preparer.prepare(ds)
      }
    } yield ds
  }

  val dbConfigLayer: ZLayer[PGSimpleDataSource, Nothing, DatabaseConfig] = ZLayer {
    for {
      ds <- ZIO.service[PGSimpleDataSource]
    } yield DatabaseConfig(ds.getUrl, ds.getUser, ds.getPassword)
  }

  val stubS3: ZLayer[Any, Nothing, S3] = stub(Path("/tmp/s3-data"))

  val storageCfgLayer: ZLayer[Any, Nothing, StorageConfig] =
    ZLayer.succeed(StorageConfig("foo", "bar", "baz"))

  val mockedEmailNotification: ULayer[Notifications] = ZLayer.succeed {
    new Notifications {
      override def sendVerificationEmail(
          addressee: String,
          code: String
      ): IO[ApplicationError.IntegrationError, Unit] = ZIO.unit

      override def sendSms(to: String, msg: String): IO[ApplicationError.IntegrationError, Unit] = ZIO.unit
    }
  }
}
