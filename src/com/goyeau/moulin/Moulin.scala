package com.goyeau.moulin

import com.goyeau.moulin.bsp.BspModule
import com.goyeau.moulin.bsp.BspModule.scalaBspFile
import com.goyeau.moulin.command.{CommandError, MissingEmptyParentsRewrite, Runner}
import com.goyeau.moulin.util.assertNotFunction
import scala.annotation.tailrec
import os.Path
import com.goyeau.moulin.util.scalaBuildPath
import com.goyeau.moulin.cache.PathRef
import com.goyeau.moulin.util.{getCanonicalScalaName, getSimpleScalaName}

/** Decalares a build definition by extending Moulin on a top level object.
  *
  * This is the entry point for the build definition.
  */
trait Moulin extends BspModule:
  def projectName: String = getClass.getSimpleScalaName

  def main(args: Array[String]): Unit =
    println(s"${Moulin.name} v${Moulin.version}")

    val command = args.mkString(" ")
    val commandSupplemented = command
      // Add backticks around terms with dashes `-`
      .replaceAll("""\b((?:\w+-)+\w+)\b""", "`$1`")
    executeCommand(commandSupplemented)

  @tailrec
  final private[moulin] def executeCommand(command: String, initialError: Option[CommandError] = None): Unit =
    try
      val projectCanonicalName = getClass.getCanonicalScalaName
      val result = Runner[Any](
        s"""import $projectCanonicalName.*, com.goyeau.moulin.command.WildCard.*; val any = $projectCanonicalName.any; def anyOf[Module] = $projectCanonicalName.anyOf[Module]; val all = $projectCanonicalName.all; def allOf[Module] = $projectCanonicalName.allOf[Module]
           |$command""".stripMargin
      )
      assertNotFunction(
        result,
        throw CommandError(
          s"method $command in command must be called with () argument",
          "Executed command needs to return a fully applied value (not a function)"
        )
      )
    catch
      case e @ MissingEmptyParentsRewrite(rewrite) => executeCommand(rewrite(command), initialError.orElse(Some(e)))
      case e: CommandError =>
        Console.err.println(initialError.getOrElse(e).details)
        System.exit(1)

  /** The path to the BSP connection file of Scala for the Moulin build definition.
    */
  def bspConnectionFile: PathRef = PathRef(scalaBuildPath / "moulin" / scalaBspFile)

  object bsp:
    export BspModule.setup
    inline def startServer(): Unit = BspModule.startServer(Moulin.this)

  /** Removes the .scala-build folder where all the state for the build is written. Hence starting from a fresh state.
    */
  def clean(): Unit = os.remove.all(scalaBuildPath)

object Moulin:
  val name    = getClass.getSimpleScalaName
  val version = "0.0.0"
