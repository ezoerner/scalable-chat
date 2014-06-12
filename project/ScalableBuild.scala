import sbt._

object ScalableBuild extends Build {
  lazy val akkaVersion = settingKey[String]("akka version")
  lazy val reactiveMongoVersion = settingKey[String]("reactive mongo version")
}