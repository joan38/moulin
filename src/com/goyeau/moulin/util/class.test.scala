package com.goyeau.moulin.util

import munit.FunSuite
import classtest.*

class classTest extends FunSuite:
  test("getSimpleScalaName should return the name of an object"):
    val name = SomeObject.getClass.getSimpleScalaName
    assertEquals(name, "SomeObject")

  test("getSimpleScalaName should return the name of a class"):
    val name = classOf[SomeClass].getSimpleScalaName
    assertEquals(name, "SomeClass")

  test("getSimpleScalaName should return the name of a trait"):
    val name = classOf[SomeTrait].getSimpleScalaName
    assertEquals(name, "SomeTrait")

  test("getSimpleScalaName should return the name of an inner class"):
    val name = classOf[OuterClass#InnerClass].getSimpleScalaName
    assertEquals(name, "InnerClass")

  test("getCanonicalScalaName should return the canonical name of an object"):
    val name = SomeObject.getClass.getCanonicalScalaName
    assertEquals(name, "com.goyeau.moulin.util.classtest.SomeObject")

  test("getCanonicalScalaName should return the canonical name of a class"):
    val name = classOf[SomeClass].getCanonicalScalaName
    assertEquals(name, "com.goyeau.moulin.util.classtest.SomeClass")

  test("getCanonicalScalaName should return the canonical name of a trait"):
    val name = classOf[SomeTrait].getCanonicalScalaName
    assertEquals(name, "com.goyeau.moulin.util.classtest.SomeTrait")

  test("getCanonicalScalaName should return the canonical name of an inner class"):
    val name = classOf[OuterClass#InnerClass].getCanonicalScalaName
    assertEquals(name, "com.goyeau.moulin.util.classtest.OuterClass.InnerClass")

package classtest:
  object SomeObject
  class SomeClass
  trait SomeTrait
  class OuterClass:
    class InnerClass
