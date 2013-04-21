import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName         = "play-emberjs-sample"
  val appVersion      = "1.0-SNAPSHOT"

  val appDependencies = Seq.empty

  val main = play.Project(appName, appVersion, appDependencies).settings(
    // Add your own project settings here      
  )

}
