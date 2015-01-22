import sbt._
import Keys._
import sbtassembly.Plugin._
import AssemblyKeys._

assemblySettings

name := "broadway"

organization := "com.ldaniels528"

version := "0.2"

scalaVersion := "2.10.4"

seq(sbtavro.SbtAvro.avroSettings: _*)

(version in avroConfig) := "1.7.7"

(stringType in avroConfig) := "String"

(sourceDirectory in avroConfig) := file("src/main/resources/avro")

scalacOptions ++= Seq("-deprecation", "-encoding", "UTF-8", "-feature", "-target:jvm-1.7", "-unchecked",
  "-Ywarn-adapted-args", "-Ywarn-value-discard", "-Xlint")

javacOptions ++= Seq("-Xlint:deprecation", "-Xlint:unchecked", "-source", "1.7", "-target", "1.7", "-g:vars")

mainClass in assembly := Some("com.ldaniels528.broadway.BroadwayServer")

test in assembly := {}

jarName in assembly := "broadway.jar"

mergeStrategy in assembly <<= (mergeStrategy in assembly) { (old) =>
  {
    case PathList("stax", "stax-api", xs @ _*) => MergeStrategy.first
    case PathList("log4j-over-slf4j", xs @ _*) => MergeStrategy.discard
    case PathList("META-INF", "MANIFEST.MF", xs @ _*) => MergeStrategy.discard
    case x => MergeStrategy.first
  }
}

// Shocktrade Dependencies
libraryDependencies ++= Seq(
  "com.ldaniels528" %% "tabular" % "0.1.0",
  "com.ldaniels528" %% "trifecta" % "0.18.14"
)

// General Dependencies
libraryDependencies ++= Seq(
  "com.twitter" %% "bijection-core" % "0.7.1",
  "com.twitter" %% "bijection-avro" % "0.7.1",
  "com.typesafe.akka" %% "akka-actor" % "2.3.8",
  "net.liftweb" %% "lift-json" % "3.0-M1",
  "org.apache.avro" % "avro" % "1.7.7",
  "org.apache.curator" % "curator-framework" % "2.7.0",
  "org.apache.httpcomponents" % "httpcore" % "4.3.2",
  "org.apache.httpcomponents" % "httpmime" % "4.3.2",
  "org.apache.kafka" %% "kafka" % "0.8.1.1"
    exclude("org.apache.zookeeper", "zookeeper")
    exclude("org.slf4j", "log4j-over-slf4j"),
  "org.apache.zookeeper" % "zookeeper" % "3.4.6",
  "org.jboss" % "jboss-vfs" % "3.2.8.Final",
  "org.slf4j" % "slf4j-api" % "1.7.9",
  "org.slf4j" % "log4j-over-slf4j" % "1.7.9",
  "net.databinder.dispatch" %% "dispatch-core" % "0.11.2"
)

// Testing Dependencies
libraryDependencies ++= Seq(
  "org.mockito" % "mockito-all" % "1.9.5" % "test",
  "org.scalatest" %% "scalatest" % "2.2.2" % "test"
)

// define the resolvers
resolvers ++= Seq(
  "Java Net" at "http://download.java.net/maven/2/",
  "Maven Central Server" at "http://repo1.maven.org/maven2",
  "Typesafe Releases Repository" at "http://repo.typesafe.com/typesafe/releases/",
  "Typesafe Snapshots Repository" at "http://repo.typesafe.com/typesafe/snapshots/",
  "Clojars Releases" at "http://clojars.org/repo/",
  "Clojure Releases" at "http://build.clojure.org/releases/",
  "Sonatype Repository" at "http://oss.sonatype.org/content/repositories/releases/"
)

resolvers += Resolver.file("Local repo", file(System.getProperty("user.home") + "/.ivy2/local"))(Resolver.ivyStylePatterns)
