package com.ketalo

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
    "1.0.0-pre.2" -> (("ember-1.0.0-pre.2.for-rhino", "handlebars-1.0.rc.1", "headless-ember-pre.2")),
    "1.0.0-rc.1" -> (("ember-1.0.0-rc.1.for-rhino", "handlebars-1.0.rc.3", "headless-ember-rc.1")),
    "1.0.0-rc.3" -> (("ember-1.0.0-rc.3.for-rhino", "handlebars-1.0.rc.3", "headless-ember-rc.1"))
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

    // load handlebars
    val handlebarsFile = loadResource(handlebars + ".js").getOrElse(throw new Exception("handlebars: could not find " + handlebars))

    try {
      ctx.evaluateReader(scope, handlebarsFile, handlebars, 1, null)
    } catch {
      case e:Exception => e.printStackTrace()
    }
    // set up global objects that emulate a browser context
    val headlessEmberFile = loadResource(headless + ".js").getOrElse(throw new Exception("headless-ember: could not find " + headless))

    try {
      ctx.evaluateReader(scope, headlessEmberFile, handlebars, 1, null)
    } catch {
      case e:Exception => e.printStackTrace()
    }
    // load ember
    val emberFile = loadResource(ember + ".js").getOrElse(throw new Exception("ember: could not find " + ember))

    try {
      ctx.evaluateReader(scope, emberFile, ember, 1, null)
    } catch {
      case e:Exception => e.printStackTrace()
    }

    ScriptableObject.putProperty(scope, "rawSource", source.replace("\r", ""))

    try {
      Right(ctx.evaluateString(scope, "(Ember.Handlebars.precompile(rawSource).toString())", "EmberJsCompiler", 0, null).toString)
    } catch {
      case e: JavaScriptException => {
        e.printStackTrace()
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
      val global = resources / "public" / "templates" / templateFile

      def naming(name: String) = name.replaceAll(fileReplaceRegexp, fileReplaceWith)

      val latestTimestamp = files.get.sortBy(f => FileInfo.lastModified(f).lastModified).reverse.map(f => FileInfo.lastModified(f)).headOption.getOrElse(FileInfo.lastModified(global))
      val currentInfos = files.get.map(f => f -> FileInfo.lastModified(f))
      val allFiles = (currentInfos ++ Seq((global, latestTimestamp))).toMap

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
        val allTemplates = generated ++ Seq(global -> global)

        Sync.writeInfo(cacheFile,
          Relation.empty[java.io.File, java.io.File] ++ allTemplates,
          allFiles)(FileInfo.lastModified.format)

        allTemplates.map(_._2).distinct.toSeq
      } else {
        previousGeneratedFiles.toSeq
      }
  }

}
