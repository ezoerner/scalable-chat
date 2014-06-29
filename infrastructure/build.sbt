parallelExecution in Test := false

libraryDependencies ++= {
  Seq(
    "org.reactivemongo"   %%   "reactivemongo-bson"      % reactiveMongoVersion.value,
    "org.specs2"          %%  "specs2"                   % "2.3.12" % "test",
    "com.typesafe.akka"   %%  "akka-actor"               % akkaVersion.value
    //"com.eaio.uuid"       %   "uuid"                     % "3.2"
  )
}
