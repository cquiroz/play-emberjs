package com.ketalo

import java.io._
import org.apache.commons.io.FilenameUtils
import org.mozilla.javascript.tools.shell.Global
import org.mozilla.javascript.Context
import org.mozilla.javascript.JavaScriptException
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject

import sbt._
import PlayProject._
import scala.Left
import scala.Right
import scala.Some
import sbt.PlayExceptions.AssetCompilationException
import java.io.File

trait EmberJsTasks extends EmberJsKeys {

  private def loadResource(name: String): Option[Reader] = {
    Option(this.getClass.getClassLoader.getResource(name)).map(_.openConnection().getInputStream).map(s => new InputStreamReader(s))
  }

  def compile(name: String, source: String): Either[(String, Int, Int), String] = {

    import org.mozilla.javascript._
    import org.mozilla.javascript.tools.shell._

    import com.ketalo.EmberJsKeys

    import scala.collection.JavaConversions._

    import java.io._
    val (ember, handlebars) = ("ember-1.0.0-pre.2.for-rhino", "handlebars-1.0.rc.1")
    val ctx = Context.enter
    ctx.setLanguageVersion(org.mozilla.javascript.Context.VERSION_1_7)
    ctx.setOptimizationLevel(9)

    val global = new Global
    global.init(ctx)
    val scope = ctx.initStandardObjects(global)

    // load handlebars
    val handlebarsFile = loadResource(handlebars + ".js").getOrElse(throw new Exception("handlebars: could not find " + handlebars))

    ctx.evaluateReader(scope, handlebarsFile, handlebars, 1, null)
    // set up global objects that emulate a browser context
    val headlessEmberFile = loadResource("headless-ember.js").getOrElse(throw new Exception("handlebars: could not find " + handlebars))

    ctx.evaluateReader(scope, headlessEmberFile, handlebars, 1, null)
    // load ember
    val emberFile = loadResource(ember + ".js").getOrElse(throw new Exception("ember: could not find " + ember))

    ctx.evaluateReader(scope, emberFile, ember, 1, null)
    val precompileFunction = scope.get("precompileEmberHandlebars", scope).asInstanceOf[Function]

    ScriptableObject.putProperty(scope, "rawSource", source.replace("\r", ""))

    try {
      Right(ctx.evaluateString(scope, "(Ember.Handlebars.precompile(rawSource).toString())", "EmberJsCompiler", 0, null).toString)
    } catch {
      case e: JavaScriptException => {
        val jsError = e.getValue.asInstanceOf[Scriptable]
        val message = ScriptableObject.getProperty(jsError, "message").toString

        // dust.js has weird error reporting where the line/column are part of the message, so we have to use a Regex to find them
        val DustCompileError = ".* At line : (\\d+), column : (\\d+)".r

        message match {
          case DustCompileError(line, column) => Left(message, line.toInt, column.toInt)
          case _ => Left(message, 0, 0) // Some other weird error, we have no line/column info now.
        }
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

  lazy val EmberJsCompiler = (sourceDirectory in Compile, resourceManaged in Compile, cacheDirectory, emberJsFileRegexFrom, emberJsFileRegexTo, emberJsAssetsDir, emberJsAssetsGlob).map {
      (src, resources, cache, fileReplaceRegexp, fileReplaceWith, assetsDir, files) =>
      val cacheFile = cache / "emberjs"

      def naming(name: String) = name.replaceAll(fileReplaceRegexp, fileReplaceWith)

      val currentInfos = files.get.map(f => f -> FileInfo.lastModified(f)).toMap

      val (previousRelation, previousInfo) = Sync.readInfo(cacheFile)(FileInfo.lastModified.format)
      val previousGeneratedFiles = previousRelation._2s


      if (previousInfo != currentInfos) {

        previousGeneratedFiles.foreach(IO.delete)

        val output = new StringBuilder
        output ++= """(function() {
          var template = Ember.Handlebars.template,
              templates = Ember.TEMPLATES = Ember.TEMPLATES || {};
                 """

        val generated:Seq[(File, File)] = (files x relativeTo(assetsDir)).flatMap {
          case (sourceFile, name) => {
            val jsSource = compile(templateName(sourceFile.getPath, assetsDir.getPath), IO.read(sourceFile)).left.map {
              case (msg, line, column) => throw AssetCompilationException(Some(sourceFile),
                msg,
                Some(line),
                Some(column))
            }.right.get

            output ++= "\ntemplates['%s'] = template(%s);\n\n".format(FilenameUtils.removeExtension(name), jsSource)

            val out = new File(resources, "public/javascripts/" + naming(name))
            IO.write(out, jsSource)
            Seq(sourceFile -> out)
          }
        }

        val global = new File(resources, "public/javascripts/templates.pre.js")
        output ++= "})();\n"
        IO.write(global, output.toString)
        val allTemplates = generated ++ Seq(global -> global)

        Sync.writeInfo(cacheFile,
          Relation.empty[java.io.File, java.io.File] ++ generated,
          currentInfos)(FileInfo.lastModified.format)

        allTemplates.map(_._2).distinct.toSeq
      } else {
        previousGeneratedFiles.toSeq
      }
  }

}

/*case class AssetCompilationExceptio(message: Option[String], atLine: Option[Int]) extends PlayException.ExceptionSource(
  "Compilation error", ~message) {
  def line = ~atLine
  def position = 0
  def input = scalax.file.Path(file).toString
  def sourceName = file.getAbsolutePath
}*/
