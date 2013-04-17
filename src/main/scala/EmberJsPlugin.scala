package com.ketalo

import sbt._
import sbt.Keys._
import org.apache.commons.io.FilenameUtils

object EmberJsPlugin extends Plugin with EmberJsTasks {

  val emberJsSettings = Seq(
    emberJsAssetsDir <<= (sourceDirectory in Compile)(src => (src / "assets" / "templates")),
    emberJsFileEnding := ".handlebars",
    emberJsAssetsGlob <<= (emberJsAssetsDir)(assetsDir => (assetsDir ** "*.handlebars")),
    emberJsFileRegexFrom <<= (emberJsFileEnding)(fileEnding => fileEnding),
    emberJsFileRegexTo <<= (emberJsFileEnding)(fileEnding => FilenameUtils.removeExtension(fileEnding) + ".js"),
    resourceGenerators in Compile <+= EmberJsCompiler
  )

  override def projectSettings: Seq[Setting[_]] = super.projectSettings ++ emberJsSettings

}