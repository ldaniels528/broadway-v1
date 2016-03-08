import sbt.Keys._
import sbt._
import sbtassembly.Plugin.AssemblyKeys._
import sbtassembly.Plugin._

val myScalaVersion = "2.11.7"
val myAkkaVersion = "2.3.14"
val myPlayVersion = "2.4.6"
val sprayVersion = "1.3.2"

val myScalacOptions = Seq("-deprecation", "-encoding", "UTF-8", "-feature", "-target:jvm-1.8", "-unchecked",
  "-Ywarn-adapted-args", "-Ywarn-value-discard", "-Xlint")
val myJavacOptions = Seq("-Xlint:deprecation", "-Xlint:unchecked", "-source", "1.8", "-target", "1.8", "-g:vars")

lazy val scalaJsOutputDir = Def.settingKey[File]("Directory for Javascript files output by ScalaJS")

lazy val broadway_js = (project in file("app-js"))
  .settings(
    name := "broadway_js",
    organization := "com.github.ldaniels528",
    version := "0.1.0",
    scalaVersion := myScalaVersion,
    relativeSourceMaps := true,
    persistLauncher := true,
    persistLauncher in Test := false,
    resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
    addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full),
    libraryDependencies ++= Seq(
      "be.doeraene" %%% "scalajs-jquery" % "0.9.0",
      "com.github.ldaniels528" %%% "scalascript" % "0.2.20",
      "com.vmunier" %% "play-scalajs-sourcemaps" % "0.1.0" exclude("com.typesafe.play", "play_2.11"),
      "org.scala-js" %%% "scalajs-dom" % "0.9.0"
    ))
  .enablePlugins(ScalaJSPlugin)

lazy val broadway_cli = (project in file("app-cli"))
  .settings(
    name := "broadway_cli",
    organization := "com.github.ldaniels528",
    version := "0.1.0",
    scalaVersion := myScalaVersion,
    scalacOptions ++= myScalacOptions,
    javacOptions ++= myJavacOptions,
    assemblySettings,
    mainClass in assembly := Some("com.github.ldaniels528.broady.cli.BroadwayREPL"),
    test in assembly := {},
    jarName in assembly := "broadway_" + version.value + ".bin.jar",
    mergeStrategy in assembly <<= (mergeStrategy in assembly) {
      (old) => {
        case PathList("stax", "stax-api", xs@_*) => MergeStrategy.first
        case PathList("log4j-over-slf4j", xs@_*) => MergeStrategy.discard
        case PathList("META-INF", "MANIFEST.MF", xs@_*) => MergeStrategy.discard
        case _ => MergeStrategy.first
      }
    },
    resolvers += "google-sedis-fix" at "http://pk11-scratch.googlecode.com/svn/trunk",
    resolvers += "clojars" at "https://clojars.org/repo",
    resolvers += "conjars" at "http://conjars.org/repo",
    resolvers += "Local Maven" at Path.userHome.asFile.toURI.toURL + ".m2/repository",
    libraryDependencies ++= Seq(
      // ldaniels528 Dependencies
      "com.github.ldaniels528" %% "commons-helpers" % "0.1.2",
      "com.github.ldaniels528" %% "tabular" % "0.1.3",
      //
      // Microsft/Azure Dependencies
      "com.microsoft.azure" % "azure-documentdb" % "1.5.1",
      "com.microsoft.azure" % "azure-storage" % "4.0.0",
      "com.microsoft.sqlserver" % "sqljdbc4" % "4.0",
      //
      // Avro Dependencies
      "com.twitter" %% "bijection-core" % "0.9.0",
      "com.twitter" %% "bijection-avro" % "0.9.0",
      "org.apache.avro" % "avro" % "1.8.0",
      //
      // Akka dependencies
      "com.typesafe.akka" %% "akka-actor" % myAkkaVersion,
      "com.typesafe.akka" %% "akka-slf4j" % myAkkaVersion,
      "com.typesafe.akka" %% "akka-testkit" % myAkkaVersion % "test",
      //
      // Kafka and Zookeeper Dependencies
      "org.apache.curator" % "curator-framework" % "2.7.1",
      "org.apache.curator" % "curator-test" % "2.7.1" % "test",
      "org.apache.kafka" %% "kafka" % "0.9.0.0",
      "org.apache.kafka" % "kafka-clients" % "0.9.0.0",
      "org.apache.zookeeper" % "zookeeper" % "3.4.7",
      //
      // Type-Safe dependencies
      "com.typesafe.play" %% "play-json" % myPlayVersion,
      //
      // SQL/NOSQL Dependencies
      "com.datastax.cassandra" % "cassandra-driver-core" % "3.0.0",
      "org.mongodb" %% "casbah-commons" % "3.1.0",
      "org.mongodb" %% "casbah-core" % "3.1.0",
      //
      // General Java Dependencies
      "commons-io" % "commons-io" % "2.4",
      //"jline" % "jline" % "2.12",
      "joda-time" % "joda-time" % "2.9.1",
      "net.liftweb" %% "lift-json" % "3.0-M7",
      "org.joda" % "joda-convert" % "1.8.1",
      "org.slf4j" % "slf4j-api" % "1.7.16",
      "org.slf4j" % "slf4j-log4j12" % "1.7.16",
      //
      // Testing dependencies
      "org.mockito" % "mockito-all" % "1.10.19" % "test",
      "org.scalatest" %% "scalatest" % "2.2.3" % "test"
    )
  )

lazy val broadway_ui = (project in file("app-play"))
  .dependsOn(broadway_cli, broadway_js)
  .settings(
    name := "broadway_ui",
    organization := "com.github.ldaniels528",
    version := "0.1.0",
    scalaVersion := myScalaVersion,
    scalacOptions ++= myScalacOptions,
    javacOptions ++= myJavacOptions,
    relativeSourceMaps := true,
    scalaJsOutputDir := (crossTarget in Compile).value / "classes" / "public" / "javascripts",
    pipelineStages := Seq(gzip, uglify),
    Seq(packageScalaJSLauncher, fastOptJS, fullOptJS) map { packageJSKey =>
      crossTarget in(broadway_js, Compile, packageJSKey) := scalaJsOutputDir.value
    },
    compile in Compile <<=
      (compile in Compile) dependsOn (fastOptJS in(broadway_js, Compile)),
    ivyScala := ivyScala.value map (_.copy(overrideScalaVersion = true)),
    resolvers += "google-sedis-fix" at "http://pk11-scratch.googlecode.com/svn/trunk",
    libraryDependencies ++= Seq(cache, filters, json, ws,
      //
      // Web Jar dependencies
      //
      "org.webjars" % "angularjs" % "1.4.8",
      //      "org.webjars" % "angularjs-nvd3-directives" % "0.0.7-1",
      "org.webjars" % "angularjs-toaster" % "0.4.8",
      //      "org.webjars" % "angular-highlightjs" % "0.4.3",
      "org.webjars" % "angular-ui-bootstrap" % "0.14.3",
      "org.webjars" % "angular-ui-router" % "0.2.13",
      "org.webjars" % "bootstrap" % "3.3.6",
      //"org.webjars" % "d3js" % "3.5.3",
      "org.webjars" % "font-awesome" % "4.5.0",
      //      "org.webjars" % "highlightjs" % "8.7",
      "org.webjars" % "jquery" % "2.1.3",
      "org.webjars" % "nervgh-angular-file-upload" % "2.1.1",
      "org.webjars" %% "webjars-play" % "2.4.0-1"
    ))
  .enablePlugins(PlayScala, play.twirl.sbt.SbtTwirl, SbtWeb)
  .aggregate(broadway_js)

lazy val broadway_tomcat = (project in file("app-tomcat"))
  .dependsOn(broadway_cli)
  .enablePlugins(TomcatPlugin)
  //.aggregate(broadway_ui)
  .settings(
  name := "broadway_tomcat",
  organization := "com.github.ldaniels528",
  version := "0.1.0",
  scalaVersion := myScalaVersion,
  scalacOptions ++= myScalacOptions,
  javacOptions ++= myJavacOptions,
  containerShutdownOnExit := false,
  libraryDependencies ++= Seq(
    "io.spray" %% "spray-can" % sprayVersion,
    "io.spray" %% "spray-servlet" % sprayVersion,
    "io.spray" %% "spray-routing" % sprayVersion,
    "io.spray" %% "spray-json" % "1.3.1",
    "io.spray" %% "spray-testkit" % sprayVersion % "test",
    "javax.servlet" % "javax.servlet-api" % "3.0.1" % "provided",
    //
    // Web Jar dependencies
    //
    "org.webjars" % "angularjs" % "1.4.8",
    //      "org.webjars" % "angularjs-nvd3-directives" % "0.0.7-1",
    "org.webjars" % "angularjs-toaster" % "0.4.8",
    //      "org.webjars" % "angular-highlightjs" % "0.4.3",
    "org.webjars" % "angular-ui-bootstrap" % "0.14.3",
    "org.webjars" % "angular-ui-router" % "0.2.13",
    "org.webjars" % "bootstrap" % "3.3.6",
    //"org.webjars" % "d3js" % "3.5.3",
    "org.webjars" % "font-awesome" % "4.5.0",
    //      "org.webjars" % "highlightjs" % "8.7",
    "org.webjars" % "jquery" % "2.1.3",
    "org.webjars" % "nervgh-angular-file-upload" % "2.1.1",
    "org.webjars" %% "webjars-play" % "2.4.0-1"
  )
)

// loads the jvm project at sbt startup
onLoad in Global := (Command.process("project broadway_tomcat", _: State)) compose (onLoad in Global).value

