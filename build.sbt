organization in ThisBuild := "scalable"

scalacOptions in ThisBuild := Seq("-unchecked", "-deprecation", "-feature", "-encoding", "utf8")

scalaVersion in ThisBuild := "2.10.3"

akkaVersion in ThisBuild := "2.3.3"

parallelExecution in Test := false

reactiveMongoVersion in ThisBuild := "0.10.0"

version in ThisBuild := "1.0.0-SNAPSHOT"

lazy val infrastructure = project

lazy val client = project.dependsOn(server % "test->compile",
                                                  infrastructure )

lazy val server = project.dependsOn(infrastructure)

lazy val scalable =
  project.in(file("."))
    .aggregate(infrastructure, client, server)

