name := "dota-assistant"

version := "1.0"

scalaVersion := "2.12.2"

libraryDependencies ++= Seq(
  "org.scala-lang.modules" %% "scala-swing" % "2.0.0",
  "net.ruippeixotog" %% "scala-scraper" % "2.0.0-RC2",
  "org.scalactic" %% "scalactic" % "3.0.1",
  "org.scalatest" %% "scalatest" % "3.0.1" % "test"
)