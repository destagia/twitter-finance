name := """twitter-finance"""
version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.6"

libraryDependencies ++= Seq(
  "com.typesafe.play" %  "play-cache_2.11"      % "2.3.0",
  "org.twitter4j"     %  "twitter4j-core"       % "4.0.2",
  "org.scalaz"        %% "scalaz-core"          % "7.1.2",
  "log4j"             %  "log4j"                % "1.2.14",
  "org.reactivemongo" %% "reactivemongo"        % "0.10.5.0.akka23"
)

scalacOptions ++= Seq("-feature")
