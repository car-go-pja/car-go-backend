import Dependencies._

import dev.guardrail.sbt.Keys.guardrailTasks

ThisBuild / scalaVersion := "2.13.8"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "com.cargo"
ThisBuild / organizationName := "cargo"

Global / onChangedBuildSource := ReloadOnSourceChanges

def openApiGeneratorSettings(yaml: String, packageName: String) = Seq(
  openApiGeneratorName := "scala-sttp",
  openApiInputSpec := yaml,
  openApiApiPackage := s"$packageName.api",
  openApiModelPackage := s"$packageName.model",
  openApiInvokerPackage := s"$packageName.core",
  openApiOutputDir := ((Compile / baseDirectory).value / "generated").absolutePath,
//  openApiAdditionalProperties := Map("modelPropertyNaming" -> "snake_case"),
  Compile / sourceGenerators += Def
    .task((file(openApiOutputDir.value) / "src" / "main" / "scala" ** "*" filter (!_.isDirectory)).get())
    .dependsOn(openApiGenerate)
    .taskValue,
  Compile / managedSourceDirectories += file(openApiOutputDir.value) / "src" / "main" / "scala",
  cleanFiles += file(openApiOutputDir.value),
  libraryDependencies ++= Seq(
    "com.softwaremill.sttp.client" %% "core" % "2.0.0",
    "com.softwaremill.sttp.client" %% "json4s" % "2.0.0",
    "com.softwaremill.sttp.client" %% "okhttp-backend" % "2.0.0",
    "com.softwaremill.sttp.client" %% "play-json" % "2.0.0",
    "org.json4s" %% "json4s-jackson" % "3.6.7"
  )
)

lazy val app = (project in file("app"))
  .enablePlugins(GuardrailPlugin, DockerPlugin, JavaServerAppPackaging, FlywayPlugin)
  .settings(
    name := "car-go",
    libraryDependencies ++= coreDeps,
    Compile / guardrailTasks := List(
      ScalaServer(
        (Compile / resourceDirectory).value / "api.yaml",
        pkg = "com.cargo.api.generated",
        framework = "http4s",
        dto = "dto"
      )
    ),
    addCompilerPlugin("org.typelevel" % "kind-projector" % "0.13.2" cross CrossVersion.full),
    scalacOptions ++= Seq(
      "-encoding",
      "utf8",
      "-Ylog-classpath"
    ),
  )
  .settings(dockerSettings)
  .settings(flywaySettings)

lazy val dockerSettings =
  Seq(
    dockerBaseImage := "openjdk:17-jdk-slim",
    dockerRepository := Some("s22630"),
    Docker / packageName := "car-go-backend",
    Docker / daemonUserUid := None,
    Docker / daemonUser := "daemon",
    dockerUpdateLatest := true
  )

lazy val flywaySettings = Seq(
  flywayUrl := sys.env.getOrElse("DB_URL", "jdbc:postgresql://localhost:5432/postgres"),
  flywayUser := sys.env.getOrElse("DB_USER", "postgres"),
  flywayPassword := sys.env.getOrElse("DB_PASS", "test123")
)

lazy val root = (project in file("."))
  .settings(
    addCompilerPlugin("org.typelevel" % "kind-projector" % "0.13.2" cross CrossVersion.full)
  )
  .aggregate(app)
