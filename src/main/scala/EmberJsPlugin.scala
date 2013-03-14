package com.ketalo

import sbt._
import sbt.Keys._
import play.Project._

object EmberJsPlugin extends Plugin {
    val emberJsEntryPoints = SettingKey[PathFinder]("play-emberjs-entry-points")
    val emberJsOptions = SettingKey[Seq[String]]("play-emberjs-options")
    val emberJsWatcher = AssetsCompiler("emberjs",
        { file => (file ** "*.handlebars")},
        sassEntryPoints,
        { (name, min) => "javascripts/" + name + ".pre" + (if (min) ".min.js" else ".js")
        },
        { (file, options) => EmberJsCompiler.compile(file, options) },
        sassOptions
    )

    val emberJsSettings = Seq(
        emberJsEntryPoints <<= (sourceDirectory in Compile)(base => ((base / "assets" ** "*.handlebars") +++ (base / "assets" ** "*.handlebars") --- base / "assets" ** "_*")), 
        emberJsOptions := Seq.empty[String],
        resourceGenerators in Compile <+= emberJsWatcher
    )
}