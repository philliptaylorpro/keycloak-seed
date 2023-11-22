addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.8.21")
addSbtPlugin("org.foundweekends.giter8" % "sbt-giter8-scaffold" % "0.16.2")

libraryDependencies += "com.typesafe.play" %% "twirl-api" % "1.6.1"
excludeDependencies ++= Seq(
  ExclusionRule("com.typesafe.sbt", "sbt-native-packager")
)

addSbtPlugin("com.github.sbt" % "sbt-native-packager" % "1.9.16")

//addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.6.4")
