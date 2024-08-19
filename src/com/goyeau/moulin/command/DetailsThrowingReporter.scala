package com.goyeau.moulin.command

import dotty.tools.dotc.core.Contexts.Context
import dotty.tools.dotc.reporting.{AbstractReporter, Diagnostic}
import dotty.tools.dotc.reporting.Diagnostic.Error

class DetailsThrowingReporter extends AbstractReporter:
  def doReport(dia: Diagnostic)(using Context): Unit = dia match
    case dia: Error => throw CommandError(dia.message, removeLineNumber(messageAndPos(dia)))
    case _          =>

  private def removeLineNumber(details: String) = details.replaceFirst("\\d \\|", "  |")

class CommandError(message: String, val details: String) extends Exception(message)
