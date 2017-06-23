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
  // async http client
  "net.databinder.dispatch" %% "dispatch-core" % "0.13.0",
  // configuration
  "com.github.pureconfig" %% "pureconfig" % "0.7.2",
  // GUI
"org.scalafx" %% "scalafx" % "8.0.102-R11"
)

val circeVersion = "0.8.0"

libraryDependencies ++= Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser"
).map(_ % circeVersion)

mainClass in assembly := Some("com.pinguinson.dotaassistant.Assistant")
assemblyJarName in assembly := "assistant.jar"
assemblyMergeStrategy in assembly := {
  case PathList("META-INF", _*) => MergeStrategy.discard
  case _ => MergeStrategy.first
}