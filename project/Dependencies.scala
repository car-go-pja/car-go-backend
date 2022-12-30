import sbt._

object Dependencies {

  object v {
    val http4s = "0.23.16"
    val doobie = "1.0.0-RC2"
    val circe = "0.14.3"
    val zioLogging = "2.1.4"
    val zioConfig = "3.0.2"
    val enumeratum = "1.7.0"
    val zio = "2.0.3"
  }

  lazy val coreDeps = Seq(
    "dev.zio" %% "zio" % v.zio,
    "dev.zio" %% "zio-test" % v.zio % "it,test",
    "dev.zio" %% "zio-test-sbt" % v.zio % "it,test",
    "dev.zio" %% "zio-test-magnolia" % v.zio % "it,test",
    "io.zonky.test" % "embedded-postgres" % "1.3.1" % IntegrationTest,
    "io.zonky.test.postgres" % "embedded-postgres-binaries-darwin-amd64" % "13.4.0" % Runtime,
    "org.flywaydb" % "flyway-core" % "7.1.1" % IntegrationTest,
    "dev.zio" %% "zio-logging" % v.zioLogging,
    "dev.zio" %% "zio-logging-slf4j" % v.zioLogging,
    "dev.zio" %% "zio-config" % v.zioConfig,
    "dev.zio" %% "zio-config-typesafe" % v.zioConfig,
    "dev.zio" %% "zio-config-magnolia" % v.zioConfig,
    "dev.zio" %% "zio-interop-cats" % "3.3.0",
    "com.github.jwt-scala" %% "jwt-circe" % "9.1.2",
    "io.circe" %% "circe-core" % v.circe,
    "io.circe" %% "circe-refined" % v.circe,
    "io.circe" %% "circe-generic" % v.circe,
    "io.circe" %% "circe-generic-extras" % v.circe,
    "com.twilio.sdk" % "twilio" % "9.1.2",
    "com.sendgrid" % "sendgrid-java" % "4.9.3",
    "com.password4j" % "password4j" % "1.6.1",
    "org.tpolecat" %% "doobie-core" % v.doobie,
    "org.tpolecat" %% "doobie-refined" % v.doobie,
    "org.tpolecat" %% "doobie-postgres" % v.doobie,
    "org.tpolecat" %% "doobie-hikari" % v.doobie,
    "io.estatico" %% "newtype" % "0.4.4",
    "io.scalaland" %% "chimney" % "0.6.2",
    "org.typelevel" %% "cats-core" % "2.9.0",
    "org.typelevel" %% "cats-effect" % "3.2.2",
    "org.http4s" %% "http4s-blaze-server" % "0.23.12",
    "org.http4s" %% "http4s-circe" % v.http4s,
    "org.http4s" %% "http4s-dsl" % v.http4s,
    "org.http4s" %% "http4s-client" % v.http4s,
    "ch.qos.logback" % "logback-classic" % "1.4.4",
    "com.beachape" %% "enumeratum" % v.enumeratum,
    "com.beachape" %% "enumeratum-circe" % v.enumeratum
  )
}
