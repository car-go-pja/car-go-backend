import Dependencies._

import dev.guardrail.sbt.Keys.guardrailTasks

ThisBuild / scalaVersion := "2.13.8"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "com.cargo"
ThisBuild / organizationName := "cargo"

lazy val app = (project in file("app"))
  .enablePlugins(GuardrailPlugin)
  .settings(
    name := "car-go",
    libraryDependencies ++= coreDeps,
    Compile / guardrailTasks := List(
      ScalaServer(
        file("./app/src/main/resources/api.yaml"),
        pkg = "com.cargo.api.generated",
        framework = "http4s",
        dto = "API"
      )
    )
  )

lazy val root = (project in file("."))
  .aggregate(app)
