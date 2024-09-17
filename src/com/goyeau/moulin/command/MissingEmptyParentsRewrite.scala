package com.goyeau.moulin.command

/** Command rewrite that adds empty parentheses to methods that are called without parentheses.
  */
object MissingEmptyParentsRewrite:
  def unapply(error: CommandError): Option[String => String] =
    val methodInRegex = """method (.+) in .+ must be called with \(\) argument""".r.unanchored
    val methodRegex   = """method (.+) must be called with \(\) argument""".r.unanchored

    error.getMessage match
      case methodInRegex(methodName) => Some(_.replaceAll(s"($methodName)\\b", "$1()"))
      case methodRegex(methodName)   => Some(_.replaceAll(s"($methodName)\\b", "$1()"))
      case _                         => None
