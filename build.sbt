name := "rn-github-api-test"
version := "0.1"
scalaVersion := "2.12.6"

libraryDependencies ++= Seq(
  "org.scalaj" %% "scalaj-http" % "2.3.0",
  "org.scalatest" %% "scalatest" % "3.0.3",
  "com.typesafe.play" %% "play-json" % "2.6.7",
  "org.slf4j" % "slf4j-log4j12" % "1.2"
)
