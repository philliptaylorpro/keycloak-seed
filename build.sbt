name := """keycloak-seed"""
organization := "net.philliptaylor"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

val silhouetteVersion = "8.0.2"

scalaVersion := "2.13.12"

libraryDependencies += guice
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "5.0.0" % Test

// Added in tutorial
resolvers += Resolver.jcenterRepo

libraryDependencies ++= Seq(
  "net.codingwell" %% "scala-guice" % "4.2.11",
  "io.github.honeycomb-cheesecake" %% "play-silhouette" % silhouetteVersion,
  "io.github.honeycomb-cheesecake" %% "play-silhouette-password-bcrypt" % silhouetteVersion,
  "io.github.honeycomb-cheesecake" %% "play-silhouette-persistence" % silhouetteVersion,
  "io.github.honeycomb-cheesecake" %% "play-silhouette-crypto-jca" % silhouetteVersion,
  "io.github.honeycomb-cheesecake" %% "play-silhouette-testkit" % silhouetteVersion % "test"
)

// Adds additional packages into Twirl
//TwirlKeys.templateImports += "net.philliptaylor.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "net.philliptaylor.binders._"
