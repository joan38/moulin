package com.goyeau.moulin.command

/** Command rewrite that adds empty parentheses to methods that are called without parentheses.
  */
object MissingEmptyParentsRewrite:
  def unapply(error: CommandError): Option[String => String] =
    val regex = """method (.+) in .+ must be called with \(\) argument""".r.unanchored
    error.getMessage match
      case regex(methodName) => Some(_.replaceAll(s"($methodName)\\b", "$1()"))
      case _                 => None
