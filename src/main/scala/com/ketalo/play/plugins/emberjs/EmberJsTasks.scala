package com.ketalo.play.plugins.emberjs

import java.io._
import org.apache.commons.io.FilenameUtils

import sbt._
import scala.Left
import scala.Right
import scala.Some
import sbt.PlayExceptions.AssetCompilationException
import java.io.File

trait EmberJsTasks extends EmberJsKeys {
  val versions = Map(
    "1.0.0-pre.2"  -> (("ember-1.0.0-pre.2.for-rhino", "handlebars-1.0.rc.1", "headless-ember-pre.2")),
    "1.0.0-rc.1"   -> (("ember-1.0.0-rc.1.for-rhino", "handlebars-1.0.rc.3", "headless-ember-rc.1")),
    "1.0.0-rc.3"   -> (("ember-1.0.0-rc.3.for-rhino", "handlebars-1.0.rc.3", "headless-ember-rc.1")),
    "1.0.0-rc.4"   -> (("ember-1.0.0-rc.4.for-rhino", "handlebars-1.0.rc.4", "headless-ember-rc.1")),
    "1.0.0-rc.5"   -> (("ember-1.0.0-rc.5.for-rhino", "handlebars-1.0.rc.4", "headless-ember-rc.1")),
    "1.0.0-rc.6"   -> (("ember-1.0.0-rc.6.for-rhino", "handlebars-1.0.rc.4", "headless-ember-rc.1")),
    "1.0.0-rc.7"   -> (("ember-1.0.0-rc.7.for-rhino", "handlebars-1.0.0", "headless-ember-rc.1"))
  )

  private def loadResource(name: String): Option[Reader] = {
    Option(this.getClass.getClassLoader.getResource(name)).map(_.openConnection().getInputStream).map(s => new InputStreamReader(s))
  }

  def compile(version:String, name: String, source: String): Either[(String, Int, Int), String] = {

    import org.mozilla.javascript._
    import org.mozilla.javascript.tools.shell._

    val (ember, handlebars, headless) = versions.get(version).getOrElse(("", "", ""))
    val ctx = Context.enter
    ctx.setLanguageVersion(org.mozilla.javascript.Context.VERSION_1_7)
    ctx.setOptimizationLevel(9)

    val global = new Global
    global.init(ctx)
    val scope = ctx.initStandardObjects(global)

    def loadScript(script: String) {
      // load handlebars
      val scriptFile = loadResource(script + ".js").getOrElse(throw new Exception("script: could not find " + script))

      try {
        ctx.evaluateReader(scope, scriptFile, script, 1, null)
      } catch {
        case e: Exception => e.printStackTrace()
      }
    }
    loadScript(handlebars)
    loadScript(headless)
    loadScript(ember)

    ScriptableObject.putProperty(scope, "rawSource", source.replace("\r", ""))

    try {
      Right(ctx.evaluateString(scope, "(Ember.Handlebars.precompile(rawSource).toString())", "EmberJsCompiler", 0, null).toString)
    } catch {
      case e: JavaScriptException => {
        Left(e.details(), e.lineNumber(), 0)
      }
      case e: org.mozilla.javascript.EcmaError => {
        Left(e.details(), e.lineNumber(), 0)
      }
    }
  }

  protected def templateName(sourceFile: String, assetsDir: String): String = {
    val sourceFileWithForwardSlashes = FilenameUtils.separatorsToUnix(sourceFile)
    val assetsDirWithForwardSlashes  = FilenameUtils.separatorsToUnix(assetsDir)
    FilenameUtils.removeExtension(
      sourceFileWithForwardSlashes.replace(assetsDirWithForwardSlashes + "/", "")
    )
  }

  import Keys._

  lazy val EmberJsCompiler = (sourceDirectory in Compile, resourceManaged in Compile, cacheDirectory, emberJsVersion, emberJsTemplateFile, emberJsFileRegexFrom, emberJsFileRegexTo, emberJsAssetsDir, emberJsAssetsGlob).map {
      (src, resources, cache, version, templateFile, fileReplaceRegexp, fileReplaceWith, assetsDir, files) =>
      val cacheFile = cache / "emberjs"
      val templatesDir = resources / "public" / "templates"
      val global = templatesDir / templateFile
      val globalMinified = templatesDir / (FilenameUtils.removeExtension(templateFile) + ".min.js")

      def naming(name: String) = name.replaceAll(fileReplaceRegexp, fileReplaceWith)

      val latestTimestamp = files.get.sortBy(f => FileInfo.lastModified(f).lastModified).reverse.map(f => FileInfo.lastModified(f)).headOption.getOrElse(FileInfo.lastModified(global))
      val currentInfos = files.get.map(f => f -> FileInfo.lastModified(f))
      val allFiles = (currentInfos ++ Seq(global -> latestTimestamp, globalMinified -> latestTimestamp)).toMap

      val (previousRelation, previousInfo) = Sync.readInfo(cacheFile)(FileInfo.lastModified.format)
      val previousGeneratedFiles = previousRelation._2s

      if (previousInfo != allFiles) {
        previousGeneratedFiles.foreach(IO.delete)

        val output = new StringBuilder
        output ++= """(function() {
          var template = Ember.Handlebars.template,
              templates = Ember.TEMPLATES = Ember.TEMPLATES || {};
                 """

        val generated:Seq[(File, File)] = (files x relativeTo(assetsDir)).flatMap {
          case (sourceFile, name) => {
            val jsSource = compile(version, templateName(sourceFile.getPath, assetsDir.getPath), IO.read(sourceFile)).left.map {
              case (msg, line, column) => throw AssetCompilationException(Some(sourceFile),
                msg,
                Some(line),
                Some(column))
            }.right.get

            output ++= "\ntemplates['%s'] = template(%s);\n\n".format(FilenameUtils.removeExtension(name), jsSource)

            val out = new File(resources, "public/templates/" + naming(name))
            IO.write(out, jsSource)
            Seq(sourceFile -> out)
          }
        }

        output ++= "})();\n"
        IO.write(global, output.toString)

        // Minify
        val minified = play.core.jscompile.JavascriptCompiler.minify(output.toString, None)
        IO.write(globalMinified, minified)

        val allTemplates = generated ++ Seq(global -> global, globalMinified -> globalMinified)

        Sync.writeInfo(cacheFile,
          Relation.empty[java.io.File, java.io.File] ++ allTemplates,
          allFiles)(FileInfo.lastModified.format)

        allTemplates.map(_._2).distinct.toSeq
      } else {
        previousGeneratedFiles.toSeq
      }
  }

}
