organization := "fr.xebia"
name := "sqs-akka-stream"

scalaVersion := "2.11.8"
val akkaVersion = "2.4.14"

scalacOptions ++= Seq(
  "-deprecation",
  "-encoding", "UTF-8",
  "-feature",
  "-language:existentials",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-unchecked",
  "-Xfatal-warnings",
  "-Xlint",
  "-Yno-adapted-args",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
  "-Xfuture"
)

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.play" %% "play-json" % "2.4.8",
  "com.typesafe.play" %% "play-ws" % "2.4.8",
  "ch.qos.logback" % "logback-classic" % "1.1.6",
  "org.slf4j" % "log4j-over-slf4j" % "1.7.21",
  "com.amazonaws" % "aws-java-sdk-sqs" % "1.11.86",

  "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test",
  "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % "test",
  "org.scalatest" %% "scalatest" % "2.2.4" % "test",
  "org.mockito" % "mockito-all" % "1.10.19" % "test",
  "com.github.tomakehurst" % "wiremock" % "1.58" % "test"
)
