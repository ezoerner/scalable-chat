parallelExecution in Test := false

libraryDependencies ++= {
  Seq(
    "com.typesafe.akka"      %%  "akka-actor"               % akkaVersion.value,
    "com.typesafe.akka"      %%  "akka-testkit"             % akkaVersion.value   % "test",
    "org.specs2"             %%  "specs2"                   % "2.3.12"            % "test",
    "com.datastax.cassandra" %   "cassandra-driver-core"    % "2.0.2"
  )
}

net.virtualvoid.sbt.graph.Plugin.graphSettings
