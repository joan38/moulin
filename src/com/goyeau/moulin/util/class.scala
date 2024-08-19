package com.goyeau.moulin.util

import scala.reflect.NameTransformer.decode

extension (`class`: Class[?])
  /** Get the simple name of the class without the Scala to Java characters (such as `$`).
    */
  def getSimpleScalaName = decode(`class`.getSimpleName).stripSuffix("$")

  /** Get the canonical name of the class without the Scala to Java characters (such as `$`).
    */
  def getCanonicalScalaName = decode(`class`.getCanonicalName).stripSuffix("$")
