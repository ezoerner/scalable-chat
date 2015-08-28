import com.typesafe.sbt.SbtScalariform.ScalariformKeys

import scalariform.formatter.preferences._

lazy val akkaVersion = "2.3.12"

lazy val commonSettings = scalariformSettings ++ resolverSettings ++
                          releaseSettings ++  // ++ publishSettings
                          net.virtualvoid.sbt.graph.Plugin.graphSettings ++
                          Seq(scalaVersion := "2.11.6",
                              scalacOptions := Seq("-unchecked", "-deprecation", "-feature", "-encoding", "utf8"),
                              parallelExecution in Test := false,
                              organization := "scalable_chat",
                              ScalariformKeys.preferences := FormattingPreferences()
                                .setPreference(AlignParameters, true)
                                .setPreference(RewriteArrowSymbols, false)
                                .setPreference(CompactControlReadability, true)
                                .setPreference(DoubleIndentClassDeclaration, true)
                                .setPreference(PreserveDanglingCloseParenthesis, true)
                                .setPreference(AlignSingleLineCaseStatements, true)
                                .setPreference(IndentLocalDefs, true)
                                .setPreference(PlaceScaladocAsterisksBeneathSecondAsterisk, true)
                                .setPreference(MultilineScaladocCommentsStartOnFirstLine, true))

lazy val resolverSettings = Seq(resolvers ++= Seq("krasserm at bintray" at "http://dl.bintray.com/krasserm/maven"))

lazy val commonSubmoduleDependencies = libraryDependencies ++= Seq(
                  "org.specs2"            %%  "specs2"                  % "2.3.12" % "test",
                  "com.typesafe.akka"     %%  "akka-actor"              % akkaVersion,
                  "ch.qos.logback"        %   "logback-classic"         % "1.1.2",
                  "org.scalactic"         %   "scalactic_2.11"          % "2.2.4")

lazy val messaging = project.settings(commonSettings: _*)
  .settings(commonSubmoduleDependencies)
  .settings(libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-stream-experimental" % "1.0"
  ))

// Fork a new JVM for 'run' and 'test:run', to avoid JavaFX double initialization problems
lazy val client = project.dependsOn(server % "test->compile",
                                    messaging )
  .settings(commonSettings: _*)
  .settings(fork := true,
            mainClass in Compile := Some("Start"),
            addCompilerPlugin("org.scalamacros" % "paradise" % "2.0.1" cross CrossVersion.full))
  .settings(commonSubmoduleDependencies)
  .settings(libraryDependencies ++= Seq(
                  "org.scalafx"         %%  "scalafx"             % "8.0.40-R8",
                  "org.scalafx"         %%  "scalafxml-core-sfx8" % "0.2.2"))

lazy val server = project.dependsOn(messaging)
  .settings(commonSettings: _*)
  .settings(commonSubmoduleDependencies)
  .settings(fork := true,
            libraryDependencies ++= Seq(
                  "com.github.krasserm"    %% "akka-persistence-cassandra"    % "0.3.9",
                  "com.typesafe.akka"      %% "akka-cluster"                  % akkaVersion,
                  "com.typesafe.akka"      %% "akka-testkit"                  % akkaVersion   % "test"))

lazy val `scalable-chat` = project.in(file("."))
    .aggregate(messaging, client, server)
    .settings(commonSettings: _*)
