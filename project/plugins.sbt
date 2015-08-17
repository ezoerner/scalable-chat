logLevel := Level.Warn

addSbtPlugin("com.github.gseitz" % "sbt-release" % "0.8.3")

scalacOptions ++= Seq("-feature", "-deprecation", "-unchecked")

addSbtPlugin("org.scalariform" % "sbt-scalariform" % "1.4.0")
