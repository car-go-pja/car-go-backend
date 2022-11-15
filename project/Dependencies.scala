import sbt._

object Dependencies {

  object v {
    val http4s = "0.23.16"
    val doobie = "1.0.0-RC2"
    val circe = "0.14.3"
  }

  lazy val coreDeps = Seq(
    "dev.zio" %% "zio" % "2.0.3",
    "dev.zio" %% "zio-interop-cats" % "13.0.0.1",
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
    "org.typelevel" %% "cats-core" % "2.9.0",
    "org.http4s" %% "http4s-blaze-server" % "0.23.12",
    "org.http4s" %% "http4s-circe" % v.http4s,
    "org.http4s" %% "http4s-dsl" % v.http4s,
    "org.http4s" %% "http4s-client" % v.http4s,
    "ch.qos.logback" % "logback-classic" % "1.2.3"
  )
}
