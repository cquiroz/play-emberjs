import com.ketalo.EmberJsKeys
import sbt._

object ApplicationBuild extends Build with EmberJsKeys {

  val appName         = "play-emberjs-sample"
  val appVersion      = "1.0-SNAPSHOT"

  val appDependencies = Seq.empty

  val main = play.Project(appName, appVersion, appDependencies).settings(
    emberJsVersion := "1.0.0-pre.2"
  )

}
