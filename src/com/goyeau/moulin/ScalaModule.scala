package com.goyeau.moulin

import com.goyeau.moulin.bsp.BspModule
import com.goyeau.moulin.bsp.BspModule.scalaBspFile
import os.{proc, pwd, Path, RelPath}
import com.goyeau.moulin.cache.Cache.cached
import com.goyeau.moulin.cache.Cache.dest
import com.goyeau.moulin.cache.PathRef
import com.goyeau.moulin.util.getSimpleScalaName

/** A ScalaModule is a module that can be compiled and run with Scala.
  */
trait ScalaModule extends BspModule:
  def moduleDir: Path = pwd / getClass.getSimpleScalaName

  /** The version of Scala used to compile this module.
    */
  def scalaVersion: String =
    proc(scalaRunner.resolveFrom(moduleDir).toString, "version", "--scala-version").call(cwd = moduleDir).out.text()

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

  /** The scala cli used to run Scala commands.
    */
  def scalaRunner = pwd / "scala"

  /** The class path of the upstream modules.
    */
  def upstreamClassPath(): String =
    dependsOn.flatMap(_.compile().split(":")).filterNot(_.contains("scala-library")).mkString(":")

  def bspConnectionFile: PathRef = cached(upstreamClassPath(), sources, generatedSources):
    (upstreamClassPath, sources, generatedSources) =>
      val _ = proc(
        Seq(scalaRunner.resolveFrom(moduleDir).toString, "setup-ide", s"--scala-version=$scalaVersion") ++
          Seq(s"--workspace=$dest", s"--semanticdb-sourceroot=$moduleDir") ++
          Seq(s"--classpath=$upstreamClassPath") ++
          scalacOptions.flatMap(option => Seq("--scalac-option", option)) ++
          (sources ++ generatedSources).map(_.path.toString)
      ).call(cwd = moduleDir, stdout = os.Inherit, stderr = os.Inherit)
      PathRef(dest / scalaBspFile)

  /** Compiles the Scala module.
    */
  def compile(): String = cached(upstreamClassPath(), sources, generatedSources):
    (upstreamClassPath, sources, generatedSources) =>
      proc(
        Seq(scalaRunner.resolveFrom(moduleDir).toString, "compile", s"--scala-version=$scalaVersion") ++
          Seq(s"--workspace=$dest", s"--semanticdb-sourceroot=$moduleDir") ++
          Seq(s"--classpath=$upstreamClassPath", "--print-classpath") ++
          scalacOptions.flatMap(option => Seq("--scalac-option", option)) ++
          (sources ++ generatedSources).map(_.path.toString)
      ).call(cwd = moduleDir, stderr = os.Inherit).out.trim()

  /** Test the Scala module.
    */
  def test(): Unit =
    val _ = proc(
      Seq(scalaRunner.resolveFrom(moduleDir).toString, "test", s"--scala-version=$scalaVersion") ++
        Seq(s"--workspace=$dest", s"--semanticdb-sourceroot=$moduleDir") ++
        Seq(s"--classpath=${upstreamClassPath()}") ++
        scalacOptions.flatMap(option => Seq("--scalac-option", option)) ++
        (sources ++ generatedSources).map(_.path.toString)
    ).call(cwd = moduleDir, stdout = os.Inherit, stderr = os.Inherit)
    ()

  /** Run the Scala module.
    */
  // def run: Unit = run()
  def run(mainClass: String = "", interactive: Boolean = false): Unit =
    val _ = proc(
      Seq(scalaRunner.resolveFrom(moduleDir).toString, "run", s"--scala-version=$scalaVersion") ++
        Seq(s"--workspace=$dest", s"--semanticdb-sourceroot=$moduleDir") ++
        Seq(s"--classpath=${upstreamClassPath()}") ++
        scalacOptions.flatMap(option => Seq("--scalac-option", option)) ++
        (if mainClass.nonEmpty then Seq(s"--main-class=$mainClass") else Seq.empty) ++
        (if interactive then Seq("--interactive") else Seq.empty) ++
        (sources ++ generatedSources).map(_.path.toString)
    ).call(cwd = moduleDir, stdout = os.Inherit, stderr = os.Inherit)
    ()

  /** Create an assembly fat jar.
    */
  def assembly(preamble: Boolean = true, force: Boolean = false): PathRef =
    cached(upstreamClassPath(), sources, generatedSources): (upstreamClassPath, sources, generatedSources) =>
      val jar = dest / "assembly.jar"
      val _ = proc(
        Seq(scalaRunner.resolveFrom(moduleDir).toString, "--power", "package", "--assembly", s"-o=$jar"),
        Seq(s"--preamble=$preamble", s"--force=$force") ++
          Seq(s"--scala-version=$scalaVersion") ++
          Seq(s"--workspace=$dest", s"--semanticdb-sourceroot=$moduleDir") ++
          Seq(s"--classpath=$upstreamClassPath") ++
          scalacOptions.flatMap(option => Seq("--scalac-option", option)) ++
          (sources ++ generatedSources).map(_.path.toString)
      ).call(cwd = moduleDir, stdout = os.Inherit, stderr = os.Inherit)
      PathRef(jar)
