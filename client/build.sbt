parallelExecution in Test := false

libraryDependencies ++= {
  Seq(
    "com.typesafe.akka"   %%  "akka-actor"              % akkaVersion.value,
    "org.scalafx"         %%  "scalafx"                 % "8.0.0-R4",
    "org.scalafx"         %%  "scalafxml-core"          % "0.2",
    "org.specs2"          %%  "specs2"                  % "2.3.12" % "test"
  )
}

addCompilerPlugin("org.scalamacros" % "paradise" % "2.0.0" cross CrossVersion.full)

// Fork a new JVM for 'run' and 'test:run', to avoid JavaFX double initialization problems
fork := true

net.virtualvoid.sbt.graph.Plugin.graphSettings

mainClass in Compile := Some("Start")