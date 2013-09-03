name := "emberjs"

version := "1.0.0-SNAPSHOT"

sbtPlugin := true

organization := "com.ketalo.play.plugins"

description := "SBT plugin for precompiling Ember.js assets in Play 2.1.x"

resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

/// Dependencies

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "1.7.1" % "test"
)

addSbtPlugin("play" % "sbt-plugin" % "2.1.0")

publishTo <<= version { v: String =>
  val nexus = "https://oss.sonatype.org/"
  if (v.trim.endsWith("SNAPSHOT"))
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases" at nexus + "service/local/staging/deploy/maven2")
}

publishMavenStyle := true

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

pomExtra := (
  <url>https://github.com/krumpi/play-emberjs</url>
  <licenses>
    <license>
      <name>BSD-style</name>
      <url>https://github.com/krumpi/play-emberjs/blob/master/LICENSE</url>
      <distribution>repo</distribution>
    </license>
  </licenses>
  <scm>
    <url>git://github.com/krumpi/play-emberjs.git</url>
    <connection>scm:git:git@github.com:krumpi/play-emberjs.git</connection>
  </scm>
  <developers>
    <developer>
      <id>krumpi</id>
      <name>Carlos Quiroz</name>
      <url>https://github.com/krumpi</url>
    </developer>
  </developers>
)