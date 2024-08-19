package com.goyeau.moulin.cache

import com.goyeau.moulin.ScalaModule
import java.io.ByteArrayOutputStream
import munit.FunSuite
import os.pwd
import os.Path
import os.SubprocessException
import scalamoduletest.*
import java.io.PrintStream

class ScalaModuleTest extends FunSuite:
  test("sources should return the correct source files"):
    assertEquals(app.sources, Seq(PathRef(projectPath / "app")))

  test("upstreamClassPath on a module without dependency should return an empty string"):
    assertEquals(lib.upstreamClassPath(), "")

  test("upstreamClassPath on a module with a dependency should return the correct class path of upstream modules"):
    val upstreamClassPath = app.upstreamClassPath()

    val libClasspathRegex =
      """.+\.scala-build/com/goyeau/moulin/cache/scalamoduletest/lib/compile/-?\d+/dest/\.scala-build/dest_\w+-\w+/classes/main:.+"""
    assert(
      upstreamClassPath.matches(libClasspathRegex),
      s"$upstreamClassPath should match the regex $libClasspathRegex"
    )

    assert(!upstreamClassPath.contains("app"), s"$upstreamClassPath should not contain 'app'")

  test("compile on a module without dependency should return the correct class path for the compiled code"):
    val compile = lib.compile()

    val libClasspathRegex =
      """.+\.scala-build/com/goyeau/moulin/cache/scalamoduletest/lib/compile/-?\d+/dest/\.scala-build/dest_\w+-\w+/classes/main:.+"""
    assert(compile.matches(libClasspathRegex), s"$compile should match the regex $libClasspathRegex")

    assert(!compile.contains("app"), s"$compile should not contain 'app'")

  test("compile on a module with a dependency should return the correct class path for the compiled code"):
    val compile = app.compile()

    val libClasspathRegex =
      """.+\.scala-build/com/goyeau/moulin/cache/scalamoduletest/lib/compile/-?\d+/dest/\.scala-build/dest_\w+-\w+/classes/main:.+"""
    assert(compile.matches(libClasspathRegex), s"$compile should match the regex $libClasspathRegex")

    val appClasspathRegex =
      """.+\.scala-build/com/goyeau/moulin/cache/scalamoduletest/app/compile/-?\d+/dest/\.scala-build/dest_\w+-\w+/classes/main:.+"""
    assert(compile.matches(appClasspathRegex), s"$compile should match the regex '$appClasspathRegex'")

  test("compile a module that fails compilation should fail"):
    intercept[SubprocessException]:
      `fail-compile`.compile()

  test("bspConnectionFile should return the correct path to the BSP connection file"):
    val bspConnectionFile = app.bspConnectionFile.path

    val regex =
      """.+\.scala-build/com/goyeau/moulin/cache/scalamoduletest/app/bspConnectionFile/-?\d+/dest/\.bsp/scala-cli.json"""
    assert(bspConnectionFile.toString.matches(regex), s"$bspConnectionFile does not match the regex $regex")
    assert(
      os.read(bspConnectionFile).contains(""""name": "scala-cli""""),
      "The BSP connection file should contain the name 'scala-cli'"
    )

  test("test should run the Scala tests"):
    lib.test()

  test("test should fail on module with no tests"):
    intercept[SubprocessException]:
      app.test()

package scalamoduletest:
  val projectPath = os.temp.dir()

  // Create all the files for the app module
  val appPath =
    val path = projectPath / "app"
    os.makeDir(path)
    os.write(path / "run.scala", """@main def run = println(hello)""")
    path

  // Create all the files for the lib module
  val libPath =
    val path = projectPath / "lib"
    os.makeDir(path)
    os.write(path / "hello.scala", """def hello = "hello"""")
    os.write(path / "project.scala", """//> using test.dependency org.scalameta::munit::1.0.1""")
    os.write(
      path / "hello.test.scala",
      """import munit.FunSuite
        |class helloTest extends FunSuite:
        |  test("hello should return hello"):
        |    assertEquals(hello, "hello")
        |""".stripMargin
    )
    path

  val failCompilePath =
    val path = projectPath / "fail-compile"
    os.makeDir(path)
    os.write(path / "failing.scala", """def fail-ing = "This is expected to fail!"""")
    path

  object app extends ScalaModule:
    override def moduleDir    = appPath
    override def scalaVersion = "3.5.0"
    override def dependsOn    = Seq(lib)

  object lib extends ScalaModule:
    override def moduleDir    = libPath
    override def scalaVersion = "3.5.0"

  object `fail-compile` extends ScalaModule:
    override def moduleDir    = failCompilePath
    override def scalaVersion = "3.5.0"
