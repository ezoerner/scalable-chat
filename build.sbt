organization in ThisBuild := "scalable"

scalacOptions in ThisBuild := Seq("-unchecked", "-deprecation", "-feature", "-encoding", "utf8")

scalaVersion in ThisBuild := "2.10.3"

akkaVersion in ThisBuild := "2.3.3"

parallelExecution in Test := false

lazy val infrastructure = project

lazy val client = project.dependsOn(server % "test->compile",
                                                  infrastructure )

lazy val server = project.dependsOn(infrastructure)

lazy val scalable =
  project.in(file("."))
    .aggregate(infrastructure, client, server)

