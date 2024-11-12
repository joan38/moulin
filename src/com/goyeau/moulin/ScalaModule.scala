package com.goyeau.moulin

import com.goyeau.moulin.bsp.BspModule
import com.goyeau.moulin.bsp.BspModule.scalaBspFile
import os.{pwd, Path, RelPath}
import com.goyeau.moulin.cache.Cache.cached
import com.goyeau.moulin.cache.Cache.dest
import com.goyeau.moulin.cache.PathRef
import com.goyeau.moulin.util.getSimpleScalaName
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import scala.cli.ScalaCli
import scala.util.Using

/** A ScalaModule is a module that can be compiled and run with Scala.
  */
trait ScalaModule extends BspModule:
  def moduleDir: Path = pwd / getClass.getSimpleScalaName

  /** The version of Scala used to compile this module.
    */
  def scalaVersion: String =
    Using.resource(ByteArrayOutputStream()): out =>
      Console.withOut(PrintStream(out)):
        ScalaCli.main(Array("version", "--scala-version", moduleDir.toString))
      out.toString.trim

  /** The options passed to the Scala compiler.
    */
  def scalacOptions: Seq[String] = Seq.empty

  /** The modules that this module depends on.
    */
  def dependsOn: Seq[ScalaModule] = Seq.empty

  /** The source files of this module.
    */
  def sources: Seq[PathRef] = Seq(moduleDir).map(PathRef(_))

  /** The generated source files of this module.
    */
  def generatedSources: Seq[PathRef] = Seq.empty

  /** The class path of the upstream modules.
    */
  def upstreamClassPath(): String =
    dependsOn.flatMap(_.compile().split(":")).filterNot(_.contains("scala-library")).mkString(":")

  def bspConnectionFile: PathRef = cached(upstreamClassPath(), sources, generatedSources):
    (upstreamClassPath, sources, generatedSources) =>
      ScalaCli.main(
        Array("setup-ide", s"--scala-version=$scalaVersion") ++
          Array(s"--workspace=$dest", s"--semanticdb-sourceroot=$moduleDir") ++
          Array(s"--classpath=$upstreamClassPath") ++
          scalacOptions.flatMap(option => Array("--scalac-option", option)) ++
          (sources ++ generatedSources).map(_.path.toString)
      )
      PathRef(dest / scalaBspFile)

  /** Compiles the Scala module.
    */
  def compile(): String = cached(upstreamClassPath(), sources, generatedSources):
    (upstreamClassPath, sources, generatedSources) =>
      Using.resource(ByteArrayOutputStream()): out =>
        Console.withOut(PrintStream(out)):
          ScalaCli.main(
            Array("compile", s"--scala-version=$scalaVersion") ++
              Array(s"--workspace=$dest", s"--semanticdb-sourceroot=$moduleDir") ++
              Array(s"--classpath=$upstreamClassPath", "--print-class-path") ++
              scalacOptions.flatMap(option => Array("--scalac-option", option)) ++
              (sources ++ generatedSources).map(_.path.toString)
          )
        out.toString.trim

  /** Test the Scala module.
    */
  def test(): Unit =
    ScalaCli.main(
      Array("test", s"--scala-version=$scalaVersion") ++
        Array(s"--workspace=$dest", s"--semanticdb-sourceroot=$moduleDir") ++
        Array(s"--classpath=${upstreamClassPath()}") ++
        scalacOptions.flatMap(option => Array("--scalac-option", option)) ++
        (sources ++ generatedSources).map(_.path.toString)
    )

  /** Run the Scala module.
    */
  // def run: Unit = run()
  def run(mainClass: String = "", interactive: Boolean = false): Unit =
    ScalaCli.main(
      Array("run", s"--scala-version=$scalaVersion") ++
        Array(s"--workspace=$dest", s"--semanticdb-sourceroot=$moduleDir") ++
        Array(s"--classpath=${upstreamClassPath()}") ++
        scalacOptions.flatMap(option => Array("--scalac-option", option)) ++
        (if mainClass.nonEmpty then Array(s"--main-class=$mainClass") else Array.empty[String]) ++
        (if interactive then Array("--interactive") else Array.empty[String]) ++
        (sources ++ generatedSources).map(_.path.toString)
    )

  /** Create an assembly fat jar.
    */
  def assembly(preamble: Boolean = true, force: Boolean = false): PathRef =
    cached(upstreamClassPath(), sources, generatedSources): (upstreamClassPath, sources, generatedSources) =>
      val jar = dest / "assembly.jar"
      ScalaCli.main(
        Array("--power", "package", "--assembly", s"-o=$jar") ++
          Array(s"--preamble=$preamble", s"--force=$force") ++
          Array(s"--scala-version=$scalaVersion") ++
          Array(s"--workspace=$dest", s"--semanticdb-sourceroot=$moduleDir") ++
          Array(s"--classpath=$upstreamClassPath") ++
          scalacOptions.flatMap(option => Array("--scalac-option", option)) ++
          (sources ++ generatedSources).map(_.path.toString)
      )
      PathRef(jar)
