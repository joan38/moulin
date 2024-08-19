package com.goyeau.moulin.cache

import io.circe.Encoder
import io.circe.Decoder
import os.Path

given Encoder[Path] = Encoder[String].contramap[Path](_.toString)
given Decoder[Path] = Decoder[String].emap(str => Right(Path(str)))
