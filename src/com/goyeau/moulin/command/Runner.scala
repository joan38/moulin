package com.goyeau.moulin.command

import com.goyeau.moulin.Moulin
import dotty.tools.dotc.Compiler
import dotty.tools.io.AbstractFile
import dotty.tools.dotc.core.Contexts.Context
import dotty.tools.dotc.Driver
import dotty.tools.dotc.util.SourceFile
import dotty.tools.io.VirtualDirectory
import dotty.tools.repl.AbstractFileClassLoader
import java.lang.ClassLoader

/** Run Scala code/command in the context of a project.
  */
object Runner:
  def apply[T](body: String): T =
    val commandRunnerFunctionName = "cmdEntryPoint"
    val code = s"""def $commandRunnerFunctionName = {
                  |$body
                  |}
                  |""".stripMargin

    val outputDirectory = VirtualDirectory("(memory)")
    compileCode(code, outputDirectory)
    val classLoader = AbstractFileClassLoader(outputDirectory, getClass.getClassLoader)
    runTopLevelFunction[T](classLoader, commandRunnerFunctionName, Seq.empty)

  private def compileCode(code: String, outputDirectory: AbstractFile): Unit =
    class DriverImpl extends Driver:
      private val compileCtx0 = initCtx.fresh

      given Context = compileCtx0.fresh
        .setSetting(compileCtx0.settings.usejavacp, true)
        .setSetting(compileCtx0.settings.outputDir, outputDirectory)
        .setReporter(DetailsThrowingReporter())
      val compiler: Compiler = newCompiler

    val driver = DriverImpl()
    import driver.given Context

    driver.compiler.newRun.compileSources(List(SourceFile.virtual(s"${Moulin.name} CLI", code)))

  private def runTopLevelFunction[T](
      classLoader: ClassLoader,
      functionName: String,
      paramClasses: Seq[Class[?]],
      arguments: Any*
  ): T =
    val module = Class.forName("$package", true, classLoader)
    val method = module.getMethod(functionName, paramClasses*)
    method.invoke(module, arguments*).asInstanceOf[T]
