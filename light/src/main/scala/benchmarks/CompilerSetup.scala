package benchmarks

import java.io.File
import java.nio.file.{Files, Path}

import benchmarks.Main.rootPath

import scala.reflect.internal.util.Position
import scala.tools.nsc.{Global, Settings}
import scala.tools.nsc.reporters.Reporter
import scala.util.Try
import collection.JavaConverters._

case class CompilerSetup(rootPath: Path) {
  val outputDir: Path = rootPath.resolve("output")
  val currentOutput: Path = outputDir.resolve("classes")
	val scalacOptions = Try(Files.readAllLines(rootPath.resolve("scalac.opts")).asScala.toList.flatMap(_.split(" +")))

  IO.cleanDir(outputDir)
  Files.createDirectories(currentOutput)


  val cpJars = IO.jarsIn(rootPath.resolve("cpJars"))

  val reporter: Reporter = new Reporter { // We are ignoring all
    override protected def info0(pos: Position, msg: String, severity: this.Severity, force: Boolean): Unit = {
    //   println(s"[$severity] $pos: $msg") // Uncomment for to get compilation messages
    }
  }

  val settings: Settings = new Settings( msg => throw new RuntimeException(s"[ERROR] $msg") )
  configure(settings)

  val global: Global = new Global(settings, reporter)

  def configure(settings: Settings): Unit = {
    settings.outputDirs.setSingleOutput(currentOutput.toString)
    settings.classpath.append(cpJars.mkString(File.pathSeparator))
	  println(s"Scalac Opts: $scalacOptions")
	  scalacOptions.foreach(opts => settings.processArguments(opts, processAll = true))
  }
}
