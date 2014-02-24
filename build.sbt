name := "emberjs"

version := "1.3.0"

sbtPlugin := true

organization := "com.ketalo.play.plugins"

description := "SBT plugin for precompiling Ember.js assets in Play 2.2.x"

resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

/// Dependencies

libraryDependencies ++= Seq()

addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.2.1")

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
  <url>https://github.com/krumpi</url>
  <licenses>
    <license>
      <name>BSD-style</name>
      <url>http://www.opensource.org/licenses/bsd-license.php</url>
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
