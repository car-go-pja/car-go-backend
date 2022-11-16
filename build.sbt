import Dependencies._

import dev.guardrail.sbt.Keys.guardrailTasks

ThisBuild / scalaVersion := "2.13.8"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "com.cargo"
ThisBuild / organizationName := "cargo"

Global / onChangedBuildSource := ReloadOnSourceChanges

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
        dto = "dto"
      )
    ),
    addCompilerPlugin("org.typelevel" % "kind-projector" % "0.13.2" cross CrossVersion.full),
    scalacOptions ++= Seq(
      "-encoding", "utf8",
      "-Ylog-classpath"
    )
  )

lazy val root = (project in file("."))
  .settings(
    addCompilerPlugin("org.typelevel" % "kind-projector" % "0.13.2" cross CrossVersion.full)
  )
  .aggregate(app)
