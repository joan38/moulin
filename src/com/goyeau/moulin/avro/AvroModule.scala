package com.goyeau.moulin.avro

import avrohugger.Generator
import avrohugger.filesorter.{AvdlFileSorter, AvscFileSorter}
import avrohugger.format.Standard
import avrohugger.format.abstractions.SourceFormat
import avrohugger.types.AvroScalaTypes
import com.goyeau.moulin.ScalaModule
import com.goyeau.moulin.util.Scala.scalaBinaryVersion
import com.goyeau.moulin.cache.Cache.cached
import com.goyeau.moulin.cache.Cache.dest
import com.goyeau.moulin.cache.PathRef
import java.io.File
import os.Path

trait AvroModule extends ScalaModule:
  def avroSources: Seq[PathRef]                     = Seq(PathRef(moduleDir / "avro"))
  def avroScalaCustomNamespace: Map[String, String] = Map.empty[String, String]
  def avroScalaFormat: SourceFormat                 = Standard
  def avroScalaCustomTypes: AvroScalaTypes          = avroScalaFormat.defaultTypes

  override def generatedSources = super.generatedSources :+ cached(avroSources): avroSources =>
    PathRef(
      generateScalaFromAvro(
        Generator(
          format = avroScalaFormat,
          avroScalaCustomTypes = Some(avroScalaCustomTypes),
          avroScalaCustomNamespace = avroScalaCustomNamespace,
          targetScalaPartialVersion = scalaBinaryVersion(scalaVersion)
        ),
        avroSources.map(_.path),
        dest / "avro"
      )
    )

  private def generateScalaFromAvro(generator: Generator, avroSources: Seq[Path], out: Path) =
    AvscFileSorter
      .sortSchemaFiles(filesFor(avroSources, "avsc"))
      .foreach: avroFile =>
        println(s"Generating case classes from AVSC $avroFile")
        generator.fileToFile(avroFile, out.toString)

    AvdlFileSorter
      .sortSchemaFiles(filesFor(avroSources, "avdl"))
      .foreach: avroFile =>
        println(s"Generating case classes from Avro IDL $avroFile")
        generator.fileToFile(avroFile, out.toString)

    filesFor(avroSources, "avro").foreach: avroFile =>
      println(s"Compiling case classes from Avro datafile $avroFile")
      generator.fileToFile(avroFile, out.toString)

    filesFor(avroSources, "avpr").foreach: avroFile =>
      println(s"Compiling case classes from Avro protocol $avroFile")
      generator.fileToFile(avroFile, out.toString)

    out

  private def filesFor(sources: Seq[Path], extension: String): Seq[File] = for
    path <- sources
    if os.exists(path)
    file <-
      if os.isDir(path) then os.walk(path).filter(file => os.isFile(file) && (file.ext == extension))
      else Seq(path)
  yield file.toIO
