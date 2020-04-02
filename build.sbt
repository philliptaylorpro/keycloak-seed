name := """keycloak-seed"""
organization := "net.philliptaylor"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.13.1"

libraryDependencies += guice
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "5.0.0" % Test

// Added in tutorial
resolvers += Resolver.jcenterRepo

libraryDependencies ++= Seq(
  "net.codingwell" %% "scala-guice" % "4.2.6",
  "com.mohiva" %% "play-silhouette" % "7.0.0",
  "com.mohiva" %% "play-silhouette-password-bcrypt" % "7.0.0",
  "com.mohiva" %% "play-silhouette-crypto-jca" % "7.0.0",
  "com.mohiva" %% "play-silhouette-persistence" % "7.0.0",
  "com.mohiva" %% "play-silhouette-testkit" % "7.0.0" % "test"
)

// Adds additional packages into Twirl
//TwirlKeys.templateImports += "net.philliptaylor.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "net.philliptaylor.binders._"
