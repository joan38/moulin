package com.goyeau.moulin.bsp

import ch.epfl.scala.bsp4j.{BspConnectionDetails, *}
import com.goyeau.moulin.Moulin
import com.goyeau.moulin.bsp.BspConnectionDetails.given
import com.goyeau.moulin.command.WildCard.allOf
import com.goyeau.moulin.cache.PathRef
import io.circe.parser.decode
import io.circe.syntax.*
import java.util.concurrent.Executors
import org.eclipse.lsp4j.jsonrpc.Launcher
import os.{proc, pwd, read, rel, write, Path}
import scala.build.bsp.ScalaScriptBuildServer
import scala.jdk.CollectionConverters.*
import scala.jdk.OptionConverters.*
import scala.quoted.*

/** A module that can be managed with a BSP client.
  */
trait BspModule:
  /** The path to the BSP connection file for this module.
    *
    * This creates the BSP connection file if necessary.
    */
  def bspConnectionFile: PathRef

object BspModule:
  trait FullBuildServer
      extends BuildServer
      with ScalaBuildServer
      with JavaBuildServer
      with JvmBuildServer
      with ScalaScriptBuildServer

  val bspFolder     = rel / ".bsp"
  val scalaBspFile  = bspFolder / "scala-cli.json"
  val moulinBspFile = pwd / bspFolder / "moulin.json"

  /** Setup the BSP connection for Moulin in the .bsp folder.
    */
  def setup(): Path =
    val commandLine = ProcessHandle.current.info.commandLine.toScala match
      case Some(commandLine) => commandLine.split("\\s").init
      // TODO: Better formulation needed
      case None => throw IllegalStateException("Unable to get the command line for running this project")

    val connectionDetails = BspConnectionDetails(
      Moulin.name,
      (commandLine :+ "bsp.startServer()").toSeq.asJava,
      Moulin.version,
      Bsp4j.PROTOCOL_VERSION,
      Seq("scala", "java").asJava
    )
    write.over(moulinBspFile, connectionDetails.asJson.spaces2, createFolders = true)
    moulinBspFile

    /** Start the BSP server for the given project.
      *
      * This will block and lisen for incoming requests on stdin and reply on stdout.
      */
  inline def startServer(inline project: BspModule): Unit =
    val executorService = Executors.newCachedThreadPool()
    val buildClient     = MoulinBuildClient()

    val scalaServerLaunchers = (project +: allBspModules(project)).map(bspModule =>
      val scalaConnectionFile = bspModule.bspConnectionFile.path
      val connectionDetails   = decode[BspConnectionDetails](read(scalaConnectionFile)).fold(throw _, identity)
      val process             = proc(connectionDetails.getArgv.asScala).spawn(cwd = pwd)

      Launcher
        .Builder[FullBuildServer]()
        .setOutput(process.stdin)
        .setInput(process.stdout)
        .setLocalService(buildClient)
        .setExecutorService(executorService)
        .setRemoteInterface(classOf[FullBuildServer])
        .create()
    )
    val scalaServers = scalaServerLaunchers.map(_.getRemoteProxy)
    // Serve a combination of all Scala modules' build server in one server
    val buildServer = MoulinBuildServer(scalaServers)

    val clientLauncher = Launcher
      .Builder[BuildClient]()
      .setOutput(System.out)
      .setInput(System.in)
      .setLocalService(buildServer)
      .setRemoteInterface(classOf[BuildClient])
      .create()

    val scalaClient = clientLauncher.getRemoteProxy
    buildClient.connect(scalaClient)

    scalaServerLaunchers.foreach(_.startListening())
    clientLauncher.startListening().get()
    ()

  private inline def allBspModules[Project](inline project: Project): Seq[BspModule] = ${ allBspModulesMacro('project) }
  def allBspModulesMacro(project: Expr[Any])(using Quotes): Expr[Seq[BspModule]] =
    import quotes.reflect.*

    def findAllBspModules(obj: Symbol): Seq[Expr[BspModule]] =
      obj.fieldMembers.filter(_.termRef <:< TypeRepr.of[BspModule]).map(Ref(_).asExprOf[BspModule]) ++
        obj.fieldMembers.flatMap(findAllBspModules)

    Expr.ofSeq(findAllBspModules(project.asTerm.underlying.symbol))
