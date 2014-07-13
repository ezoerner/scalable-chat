import com.typesafe.sbt.SbtScalariform.ScalariformKeys

import scalariform.formatter.preferences._

lazy val akkaVersion = "2.3.4"

lazy val commonSettings = scalariformSettings ++ resolverSettings ++
                          releaseSettings ++  // ++ publishSettings
                          net.virtualvoid.sbt.graph.Plugin.graphSettings ++
                          Seq(scalaVersion := "2.11.1",
                              scalacOptions := Seq("-unchecked", "-deprecation", "-feature", "-encoding", "utf8"),
                              parallelExecution in Test := false,
                              organization := "scalable_chat",
                              ScalariformKeys.preferences := FormattingPreferences()
                                .setPreference(AlignParameters, true)
                                .setPreference(RewriteArrowSymbols, true)
                                .setPreference(CompactControlReadability, true)
                                .setPreference(DoubleIndentClassDeclaration, true)
                                .setPreference(PreserveDanglingCloseParenthesis, true)
                                .setPreference(AlignSingleLineCaseStatements, true)
                                .setPreference(PlaceScaladocAsterisksBeneathSecondAsterisk, false)
                                .setPreference(MultilineScaladocCommentsStartOnFirstLine, false))

lazy val resolverSettings = Seq(resolvers ++= Seq())

lazy val commonSubmoduleDependencies = libraryDependencies ++= Seq(
                  "org.specs2"            %%  "specs2"                  % "2.3.12" % "test",
                  "com.typesafe.akka"     %%  "akka-actor"              % akkaVersion)

lazy val infrastructure = project.settings(commonSettings: _*)
  .settings(commonSubmoduleDependencies)
  .settings(libraryDependencies ++= Seq(
                  "com.datastax.cassandra" %  "cassandra-driver-core"   % "2.0.2"))

// Fork a new JVM for 'run' and 'test:run', to avoid JavaFX double initialization problems
lazy val client = project.dependsOn(server % "test->compile",
                                    infrastructure )
  .settings(commonSettings: _*)
  .settings(fork := true,
            mainClass in Compile := Some("Start"),
            addCompilerPlugin("org.scalamacros" % "paradise" % "2.0.0" cross CrossVersion.full))
  .settings(commonSubmoduleDependencies)
  .settings(libraryDependencies ++= Seq(
                  "org.scalafx"         %%  "scalafx"            % "8.0.0-R4",
                  "org.scalafx"         %%  "scalafxml-core"     % "0.2.1-SNAPSHOT"))

lazy val server = project.dependsOn(infrastructure)
  .settings(commonSettings: _*)
  .settings(commonSubmoduleDependencies)
  .settings(libraryDependencies ++= Seq(
                  "com.datastax.cassandra" %   "cassandra-driver-core"        % "2.0.2",
                  "com.typesafe.akka"      %% "akka-persistence-experimental" % akkaVersion,
                  "com.typesafe.akka"      %%  "akka-testkit"                 % akkaVersion   % "test"))

lazy val scalable =
  project.in(file("."))
    .aggregate(infrastructure, client, server)
    .settings(commonSettings: _*)

