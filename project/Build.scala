import sbt._
import sbt.Keys._

object PluginBuild extends Build {

  lazy val playEmberJS = Project(
    id = "play-emberjs", base = file(".")
  )

}
