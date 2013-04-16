package com.ketalo

import sbt._
import sbt.Keys._
import org.apache.commons.io.FilenameUtils

object EmberJsPlugin extends Plugin with EmberJsTasks {

  /*val emberJsEntryPoints = SettingKey[PathFinder]("play-emberjs-entry-points")
  val emberJsOptions = SettingKey[Seq[String]]("play-emberjs-options")
  val emberJsWatcher = AssetsCompiler("emberjs",
    { file => (file ** "*.handlebars")},
    emberJsEntryPoints,
    { (name, min) => "javascripts/" + name + ".pre" + (if (min) ".min.js" else ".js")
    },
    { (file, options) => println("precomp " + file.getParent);EmberJsCompiler.compile(file.getParentFile, options) },
    emberJsOptions
  )*/

  val emberJsSettings = Seq(
    emberJsAssetsDir <<= (sourceDirectory in Compile)(src => (src / "assets" / "templates")),
    emberJsFileEnding := ".handlebars",
    emberJsAssetsGlob <<= (emberJsAssetsDir)(assetsDir => assetsDir ** "*.handlebars"),
    emberJsFileRegexFrom <<= (emberJsFileEnding)(fileEnding => fileEnding),
    emberJsFileRegexTo <<= (emberJsFileEnding)(fileEnding => FilenameUtils.removeExtension(fileEnding) + ".js"),
    resourceGenerators in Compile <+= EmberJsCompiler
  )

  override def projectSettings: Seq[Setting[_]] = super.projectSettings ++ emberJsSettings

}