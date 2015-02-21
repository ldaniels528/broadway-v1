import sbt._
import Keys._
import sbtassembly.Plugin._
import AssemblyKeys._

assemblySettings

name := "broadway"

organization := "com.ldaniels528"

version := "0.6"

scalaVersion := "2.11.4"

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
  "com.ldaniels528" %% "trifecta" % "0.18.15"
)

// General Dependencies
libraryDependencies ++= Seq(
  "com.datastax.cassandra" % "cassandra-driver-core" % "2.1.4",
  "com.twitter" %% "bijection-core" % "0.7.2",
  "com.twitter" %% "bijection-avro" % "0.7.2",
  "com.typesafe.akka" %% "akka-actor" % "2.3.9",
  "com.typesafe.play" %% "play-ws" % "2.4.0-M2",
  "com.typesafe.slick" %% "slick" % "2.1.0",
  "mysql" % "mysql-connector-java" % "5.1.34",
  "net.liftweb" %% "lift-json" % "3.0-M3",
  "org.apache.avro" % "avro" % "1.7.7",
  "org.apache.curator" % "curator-framework" % "2.7.1",
  "org.apache.httpcomponents" % "httpcore" % "4.3.2",
  "org.apache.httpcomponents" % "httpmime" % "4.3.2",
  "org.apache.kafka" %% "kafka" % "0.8.2-beta"
    exclude("org.apache.zookeeper", "zookeeper")
    exclude("org.slf4j", "log4j-over-slf4j"),
  "org.apache.zookeeper" % "zookeeper" % "3.4.6",
  "org.jboss" % "jboss-vfs" % "3.2.8.Final",
  "org.mashupbots.socko" %% "socko-webserver" % "0.6.0",
  "org.mongodb" %% "casbah-commons" % "2.7.4",
  "org.mongodb" %% "casbah-core" % "2.7.4",
  "org.slf4j" % "slf4j-api" % "1.7.10",
  "org.slf4j" % "log4j-over-slf4j" % "1.7.10",
  "net.databinder.dispatch" %% "dispatch-core" % "0.11.2"
)

// Testing Dependencies
libraryDependencies ++= Seq(
  "org.mockito" % "mockito-all" % "1.10.19" % "test",
  "org.scalatest" %% "scalatest" % "2.2.3" % "test"
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
