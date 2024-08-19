package com.goyeau.moulin.bsp

import ch.epfl.scala.bsp4j.BspConnectionDetails
import io.circe.Encoder
import io.circe.Decoder
import io.circe.Json
import io.circe.syntax.*
import scala.jdk.CollectionConverters.*

object BspConnectionDetails:
  given Encoder[BspConnectionDetails] = details =>
    Json.obj(
      "name"       -> details.getName.asJson,
      "argv"       -> details.getArgv.asScala.asJson,
      "version"    -> details.getVersion.asJson,
      "bspVersion" -> details.getBspVersion.asJson,
      "languages"  -> details.getLanguages.asScala.asJson
    )

  given Decoder[BspConnectionDetails] = cursor =>
    for
      name       <- cursor.downField("name").as[String]
      argv       <- cursor.downField("argv").as[Seq[String]]
      version    <- cursor.downField("version").as[String]
      bspVersion <- cursor.downField("bspVersion").as[String]
      languages  <- cursor.downField("languages").as[Seq[String]]
    yield new BspConnectionDetails(name, argv.asJava, version, bspVersion, languages.asJava)
