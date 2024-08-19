package com.goyeau.moulin.command

import munit.FunSuite

class RunnerTest extends FunSuite:
  test("Runner should execute code"):
    assertEquals(Runner[String]("(1 + 4).toString"), "5")
