package com.goyeau.moulin.cache

import munit.FunSuite
import nametest.*

class NameTest extends FunSuite:
  test("Name should return the name of the function it's in"):
    val name = project.field
    assertEquals(name, "com.goyeau.moulin.cache.nametest.project.field")

  test("Name should return the name even in the case of nested fields"):
    val name = project.field2
    assertEquals(name, "com.goyeau.moulin.cache.nametest.project.field2.nested")

package nametest:
  object project extends NameModule

  trait NameModule:
    def field: String = summon[Name]

    def field2: String =
      val nested = summon[Name]
      nested
