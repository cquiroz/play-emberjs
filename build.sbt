name := "play-emberjs"

version := "0.1.0"

sbtPlugin := true

organization := "com.ketalo"

description := "SBT plugin for handling Sass assets in Play 2.1"

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
      <name>MIT-style</name>
      <url>https://github.com/krumpi/play-emberjs/blob/master/LICENSE</url>
      <distribution>repo</distribution>
    </license>
  </licenses>
  <scm>
    <url>git@github.com:krumpi/play-emberjs.git</url>
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