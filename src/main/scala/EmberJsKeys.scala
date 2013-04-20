package com.ketalo

import sbt._

trait EmberJsKeys {
    val emberJsAssetsDir = SettingKey[File]("play-emberjs-assets-dir")
    val emberJsFileEnding = SettingKey[String]("play-emberjs-file-ending")
    val emberJsAssetsGlob = SettingKey[PathFinder]("play-emberjs-assets-glob")
    val emberJsTemplateFile = SettingKey[String]("play-emberjs-template-file")
    val emberJsFileRegexFrom = SettingKey[String]("play-emberjs-file-regex-from")
    val emberJsFileRegexTo = SettingKey[String]("play-emberjs-file-regex-to")
}
