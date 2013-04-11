package com.ketalo

import java.io._
import play.api._

import scalaz._
import Scalaz._

object EmberJsCompiler {
  def compile(root: File, options: Seq[String]): (String, Option[String], Seq[File]) = new EmberJsCompiler("", "").compile(root, options)
}

class EmberJsCompiler(ember: String, handlebars: String) {

  import org.mozilla.javascript._
  import org.mozilla.javascript.tools.shell._

  import scala.collection.JavaConverters._

  import scalax.file._

  /**
   * find a file with the given name in the current directory or any subdirectory
   */
  private def findFile(name: String): Option[File] = {
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

  private lazy val compiler = {
    val ctx = Context.enter
    ctx.setLanguageVersion(org.mozilla.javascript.Context.VERSION_1_7)
    ctx.setOptimizationLevel(9)
    //ctx.setOptimizationLevel(-1)
    val global = new Global
    global.init(ctx)
    val scope = ctx.initStandardObjects(global)

    // set up global objects that emulate a browser context
    // load handlebars
    val handlebarsFile = findFile(handlebars).getOrElse(throw new Exception("handlebars: could not find " + handlebars))

    ctx.evaluateString(scope, Path(handlebarsFile).string(), handlebars, 1, null)
    // load handlebars
    val headlessEmberFile = findFile("headless-ember.js").getOrElse(throw new Exception("handlebars: could not find " + handlebars))

    ctx.evaluateString(scope, Path(headlessEmberFile).string(), handlebars, 1, null)
    // load ember
    val emberFile = findFile(ember).getOrElse(throw new Exception("ember: could not find " + ember))

    ctx.evaluateString(scope, Path(emberFile).string(), ember, 1, null)
    val precompileFunction = scope.get("precompileEmberHandlebars", scope).asInstanceOf[Function]

    Context.exit

    (source: File) => {
      val handlebarsCode = Path(source).string().replace("\r", "")
      val jsSource = Context.call(null, precompileFunction, scope, scope, Array(handlebarsCode)).asInstanceOf[String]
      (jsSource, None, Seq.empty)
    }
  }

  def compileDir(root: File, options: Seq[String]): (String, Seq[File]) = {
    println("compile dir " + options)
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
          val jsSource = compile(file, options)
          dependencies += file
          output ++= "templates['%s'] = template(%s);\n\n".format(templateName, jsSource)
        }
      }
    }
    addTemplateDir(root, "")

    output ++= "})();\n"
    (output.toString, dependencies.result)
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
        throw CompilationException(
          "unexpected exception during Ember compilation (file=%s, options=%s, ember=%s): %s".format(
            source, options, ember, e
          ),
          source,
          None)
    }
  }

}


case class CompilationException(message: String, file: File, atLine: Option[Int]) extends PlayException.ExceptionSource(
  "Compilation error", message) {
  def line = ~atLine
  def position = 0
  def input = scalax.file.Path(file).toString
  def sourceName = file.getAbsolutePath
}
