package com.goyeau.moulin.cache

import com.goyeau.moulin.util.scalaBuildPath
import sourcecode.Args
import io.circe.Encoder
import io.circe.Decoder
import io.circe.syntax.*
import io.circe.parser.*
import os.Path
import scala.quoted.*
import cats.syntax.all.*
import scala.jdk.OptionConverters.*

/** A file system cache that can be used to cache the results of a function or storing files.
  */
trait Cache:
  /** The path to the cache folder.
    */
  def path: Path

object Cache:
  /** Cache a function result on file.
    *
    * The result will be cached until:
    *   - A clean is run
    *   - The args of the function are different
    *   - The build definition is changed
    */
  def cached[Result: Encoder: Decoder](body: Cache ?=> Result)(using Cache): Result = cached(EmptyTuple)(_ => body)

  /** Cache a function result on file.
    *
    * The result will be cached until:
    *   - A clean is run
    *   - The declared dependencies' hashcode is different
    *   - The args of the function are different
    *   - The build definition is changed
    */
  def cached[Deps, Result: Encoder: Decoder](dependencies: Deps)(body: Deps => Cache ?=> Result)(using Cache): Result =
    // Refine the cache path to include dependencies' hashcode
    val refinedCache = new Cache:
      val path = summon[Cache].path / os.up / (summon[Cache].path.last, dependencies).hashCode.toString
    val cachePath = refinedCache.path / "cache.json"
    Either
      .catchNonFatal(os.read(cachePath))
      .flatMap(decode[Result](_))
      .getOrElse:
        val bodyResult = body(dependencies)(using refinedCache)
        os.write.over(cachePath, bodyResult.asJson.spaces2, createFolders = true)
        bodyResult

  /** A path to a folder that can be used to store files.
    *
    * A new folder is returned according to the parent cached function.
    */
  def dest(using Cache): Path = summon[Cache].path / "dest"

  /** A cache that is scoped to the given name and arguments.
    */
  given (using Name, Args): Cache = new Cache:
    val path =
      val functionPath    = summon[Name].split("\\.").foldLeft(os.rel)(_ / _)
      val functionArgs    = summon[Args].value.flatten.map(_.value)
      val buildDefinition = buildClasses()
      scalaBuildPath / functionPath / (functionArgs, buildDefinition).hashCode.toString

  /** PathRef to the folder that contains classes for the build definition.
    */
  private def buildClasses() = ProcessHandle.current.info.commandLine.toScala match
    case Some(commandLine) => PathRef(Path(commandLine.split("\\s|:")(2)))
    // TODO: Better formulation needed
    case None => throw IllegalStateException("Unable to get the command line for running this project")
