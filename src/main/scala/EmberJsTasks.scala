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

  import org.mozilla.javascript._
  import org.mozilla.javascript.tools.shell._

  import scalax.file._

  /**
   * find a file with the given name in the current directory or any subdirectory
   */
  /*private def findFile(name: String): Option[File] = {
    def findIn(dir: File): Option[File] = {
      for (file <- dir.listFiles) {
        if (file.isDirectory) {
          findIn(file) match {
            case Some(f) => return Some(f)
            case None => // keep trying
          }
        } else if (file.getName == name) {
          return Some(file)
        }
      }
      None
    }
    findIn(new File("."))
  }

  private def loadResource(name: String): Option[String] = {
    println(name)
    scala.io.Source.fromInputStream(this.getClass.getClassLoader.getResource(name).openConnection().getInputStream).getLines().mkString.some
  }

  private lazy val compiler = {
    val ctx = Context.enter
    //ctx.setLanguageVersion(org.mozilla.javascript.Context.VERSION_1_7)
    //ctx.setOptimizationLevel(9)
    //ctx.setOptimizationLevel(-1)
                              ctx.setGeneratingSource(true)
                              ctx.setErrorReporter(new ErrorReporter(){
                                def warning(p1: String, p2: String, p3: Int, p4: String, p5: Int) {
                                  println("warn " + p1)
                                }

                                def error(p1: String, p2: String, p3: Int, p4: String, p5: Int) {
                                  println("error " + p1)
                                }

                                def runtimeError(p1: String, p2: String, p3: Int, p4: String, p5: Int) = {
                                  println("runtimeerror " + p1)
                                  new EvaluatorException(p1)
                                }
                              })
    val global = new Global
    global.init(ctx)
    val scope = ctx.initStandardObjects(global)

    // set up global objects that emulate a browser context
    // load handlebars
    val handlebarsFile = loadResource(handlebars + ".js").getOrElse(throw new Exception("handlebars: could not find " + handlebars))

    ctx.evaluateString(scope, handlebarsFile, handlebars, 1, null)
    // load handlebars
    val headlessEmberFile = loadResource("headless-ember.js").getOrElse(throw new Exception("handlebars: could not find " + handlebars))

    ctx.evaluateString(scope, headlessEmberFile, handlebars, 1, null)
    // load ember
    val emberFile = loadResource(ember + ".js").getOrElse(throw new Exception("ember: could not find " + ember))

    ctx.evaluateString(scope, emberFile, ember, 1, null)
    val precompileFunction = scope.get("precompileEmberHandlebars", scope).asInstanceOf[Function]

    Context.exit

    (source: File) => {
      val handlebarsCode = Path(source).string().replace("\r", "")
      val jsSource = Context.call(null, precompileFunction, scope, scope, Array(handlebarsCode)).asInstanceOf[String]
      (jsSource, None, Seq.empty)
    }
  }

  def compileDir(root: File, options: Seq[String]): (String, Option[String], Seq[File]) = {
    println("compile dir " + root)
    val dependencies = Seq.newBuilder[File]

    val output = new StringBuilder
    output ++= "(function() {\n" +
      "var template = Ember.Handlebars.template,\n" +
      "    templates = Ember.TEMPLATES = Ember.TEMPLATES || {};\n\n"

    def addTemplateDir(dir: File, path: String) {
      for {
        file <- dir.listFiles.toSeq.sortBy(_.getName)
        name = file.getName
      } {
        if (file.isDirectory) {
          addTemplateDir(file, path + name + "/")
        }
        else if (file.isFile && name.endsWith(".handlebars")) {
          val templateName = path + name.replace(".handlebars", "")
          println("ember: processing template %s".format(templateName))
          println("ember: compile %s".format(file))
          val jsSource = compile(file, options)
          dependencies += file
          output ++= "templates['%s'] = template(%s);\n\n".format(templateName, jsSource)
        }
      }
    }
    addTemplateDir(root, "")

    output ++= "})();\n"
    (output.toString, None, dependencies.result)
  }

  private def compile(source: File, options: Seq[String]): (String, Option[String], Seq[File]) = {
    try {
      compiler(source)
    } catch {
      case e: JavaScriptException =>

        val line = """.*on line ([0-9]+).*""".r
        val error = e.getValue.asInstanceOf[Scriptable]

        throw ScriptableObject.getProperty(error, "message").asInstanceOf[String] match {
          case msg@line(l) => CompilationException(
            msg,
            source,
            Some(Integer.parseInt(l)))
          case msg => CompilationException(
            msg,
            source,
            None)
        }

      case e =>
        e.printStackTrace()
        throw CompilationException(
          "unexpected exception during Ember compilation (file=%s, options=%s, ember=%s): %s".format(
            source, options, ember, e
          ),
          source,
          None)
    }

  }*/

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

        // set up global objects that emulate a browser context
    // load handlebars
    val handlebarsFile = loadResource(handlebars + ".js").getOrElse(throw new Exception("handlebars: could not find " + handlebars))

    ctx.evaluateReader(scope, handlebarsFile, handlebars, 1, null)
    // load handlebars
    val headlessEmberFile = loadResource("headless-ember.js").getOrElse(throw new Exception("handlebars: could not find " + handlebars))

    ctx.evaluateReader(scope, headlessEmberFile, handlebars, 1, null)
    // load ember
    val emberFile = loadResource(ember + ".js").getOrElse(throw new Exception("ember: could not find " + ember))

    ctx.evaluateReader(scope, emberFile, ember, 1, null)
    val precompileFunction = scope.get("precompileEmberHandlebars", scope).asInstanceOf[Function]

    /*(source: File) => {
      val handlebarsCode = Path(source).string().replace("\r", "")
      val jsSource = Context.call(null, precompileFunction, scope, scope, Array(handlebarsCode)).asInstanceOf[String]
      (jsSource, None, Seq.empty)
    }*/

    /*ctx.evaluateReader(
      scope,
      new InputStreamReader(this.getClass.getClassLoader.getResource("dust-full-0.6.0.js").openConnection().getInputStream()),
      "dust.js",
      1, null)*/

    ScriptableObject.putProperty(scope, "rawSource", source.replace("\r", ""))
    ScriptableObject.putProperty(scope, "name", name)

    try {
      println(ctx.evaluateString(scope, "(precompileEmberHandlebars(rawSource).toString())", "EmberJsCompiler", 0, null).toString)
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

      //if (previousInfo != currentInfos) {

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

            output ++= "templates['%s'] = template(%s);\n\n".format(name, jsSource)

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

        allTemplates.foreach(println)
        allTemplates.map(_._2).distinct.toSeq
      /*} else {
        previousGeneratedFiles.toSeq
      }*/
  }

}

/*case class AssetCompilationExceptio(message: Option[String], atLine: Option[Int]) extends PlayException.ExceptionSource(
  "Compilation error", ~message) {
  def line = ~atLine
  def position = 0
  def input = scalax.file.Path(file).toString
  def sourceName = file.getAbsolutePath
}*/
