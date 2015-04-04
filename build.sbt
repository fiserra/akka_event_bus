name := "akka_event_bus"

version := "1.0"

scalaVersion := "2.11.6"

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.3.9"
libraryDependencies += "com.typesafe.akka" % "akka-testkit_2.11" % "2.3.9"
libraryDependencies += "org.scalatest" % "scalatest_2.11" % "2.2.4" % "test"