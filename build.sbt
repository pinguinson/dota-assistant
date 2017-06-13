name := "dota-assistant"

version := "1.0"

scalaVersion := "2.12.2"

libraryDependencies ++= Seq(
  // swing for UI
  "org.scala-lang.modules" %% "scala-swing" % "2.0.0",
  // html scraper for dotabuff
  "net.ruippeixotog" %% "scala-scraper" % "2.0.0-RC2",
  // scala test
  "org.scalactic" %% "scalactic" % "3.0.1",
  "org.scalatest" %% "scalatest" % "3.0.1" % "test",
  // http client (might wanna switch to async one)
  "org.scalaj" %% "scalaj-http" % "2.3.0",
  // configuration
  "com.github.pureconfig" %% "pureconfig" % "0.7.2"
)

val circeVersion = "0.8.0"

libraryDependencies ++= Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser"
).map(_ % circeVersion)