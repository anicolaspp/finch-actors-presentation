name := "actor-presentation"

version := "1.0"

scalaVersion := "2.12.2"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.5.2",
  "com.typesafe.akka" %% "akka-testkit" % "2.5.2" % Test

//  "com.typesafe.akka" %% "akka-http" % "10.0.7",
//  "com.typesafe.akka" %% "akka-http-testkit" % "10.0.7" % Test,
//  "com.typesafe.akka" %% "akka-http-spray-json" % "10.0.7"
)

libraryDependencies ++= Seq(
  "com.github.finagle" %% "finch-core" % "0.14.0",
  "com.github.finagle" %% "finch-generic" % "0.14.0",
  "com.github.finagle" %% "finch-circe" % "0.14.0",

  "io.circe" %% "circe-core" % "0.8.0",
  "io.circe" %% "circe-generic" % "0.8.0",
  "io.circe" %% "circe-parser" % "0.8.0"
)

libraryDependencies += "org.scalactic" %% "scalactic" % "3.0.1"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.1" % "test"