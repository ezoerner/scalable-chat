logLevel := Level.Warn

addSbtPlugin("com.github.gseitz" % "sbt-release" % "0.8.3")

scalacOptions ++= Seq("-feature", "-deprecation", "-unchecked")

addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.7.4")
