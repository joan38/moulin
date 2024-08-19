package com.goyeau.moulin.util

import classtest.*
import com.goyeau.moulin.util.Scala.scalaBinaryVersion
import munit.FunSuite

class ScalaTest extends FunSuite:
  test("scalaBinaryVersion should return '3.0.0-milestone' for Scala 3 early versions"):
    assertEquals(scalaBinaryVersion("3.0.0-milestone"), "3.0.0-milestone")

  test("scalaBinaryVersion should return '3' for Scala 3 versions"):
    assertEquals(scalaBinaryVersion("3.1.2"), "3")

  test("scalaBinaryVersion should return '2.13' for Scala 2.13.x versions"):
    assertEquals(scalaBinaryVersion("2.13.7"), "2.13")

  test("scalaBinaryVersion should return '2.12' for Scala 2.12.x versions"):
    assertEquals(scalaBinaryVersion("2.12.14"), "2.12")

  test("scalaBinaryVersion should return '0.27' for Dotty 0.27.x versions"):
    assertEquals(scalaBinaryVersion("0.27.0-RC1"), "0.27")

  test("scalaBinaryVersion should return '2.13' for Typelevel Scala 2.13.x versions"):
    assertEquals(scalaBinaryVersion("2.13.7-bin-typelevel-4"), "2.13")

  test("scalaBinaryVersion should return the same version for unknown Scala versions"):
    assertEquals(scalaBinaryVersion("2.11.12"), "2.11")
