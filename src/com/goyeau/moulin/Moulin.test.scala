package com.goyeau.moulin

import munit.FunSuite
import runnertest.*

class MoulinTest extends FunSuite:
  test("executeCommand should run a function without parenteses from a command"):
    project.runCount = 0
    project.executeCommand("runNoParenteses")
    assertEquals(project.runCount, 1)

  test("executeCommand should run a function that have parenteses from a command that have the parenteses"):
    project.runCount = 0
    project.executeCommand("runWithParenteses()")
    assertEquals(project.runCount, 1)

  test("executeCommand should run a function that have parenteses from a command that is missing the parenteses"):
    project.runCount = 0
    project.executeCommand("runWithParenteses")
    assertEquals(project.runCount, 1)

  test(
    "executeCommand should run a function that have default parameters from a command that is missing the parenteses"
  ):
    project.runCount = 0
    project.executeCommand("runWithOptionalParams")
    assertEquals(project.runCount, 2)

  test("executeCommand should run a function that have default parameters from a command that specify the parameters"):
    project.runCount = 0
    project.executeCommand("runWithOptionalParams(inc = 1)")
    assertEquals(project.runCount, 1)

package runnertest:
  object project extends Moulin:
    var runCount                            = 0
    def runNoParenteses                     = runCount += 1
    def runWithParenteses()                 = runCount += 1
    def runWithOptionalParams(inc: Int = 2) = runCount += inc
