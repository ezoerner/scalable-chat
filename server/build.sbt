parallelExecution in Test := false

libraryDependencies ++= {
  Seq(
    "com.typesafe.akka"   %%  "akka-actor"               % akkaVersion.value,
    "org.reactivemongo"   %%   "reactivemongo-bson"      % reactiveMongoVersion.value,
    "com.typesafe.akka"   %%  "akka-testkit"             % akkaVersion.value   % "test",
    "org.specs2"          %%  "specs2"                   % "2.3.12"            % "test"
  )
}