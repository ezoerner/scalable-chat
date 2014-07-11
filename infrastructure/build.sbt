parallelExecution in Test := false

libraryDependencies ++= {
  Seq(
    "org.specs2"            %%  "specs2"                  % "2.3.12" % "test",
    "com.typesafe.akka"     %%  "akka-actor"              % akkaVersion.value,
    "org.scala-lang"        %%  "scala-pickling"          % "0.8.0",
    "com.datastax.cassandra" %  "cassandra-driver-core"   % "2.0.2"
  )
}

net.virtualvoid.sbt.graph.Plugin.graphSettings
