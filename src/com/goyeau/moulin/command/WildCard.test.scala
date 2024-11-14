package com.goyeau.moulin.command

import com.goyeau.moulin.command.WildCard.*
import com.goyeau.moulin.Moulin
import com.goyeau.moulin.ScalaModule
import munit.FunSuite
import wildcardtest.*
import scala.annotation.experimental
import scala.language.dynamics

@experimental
class WildCardTest extends FunSuite:
  test("any should combine vals"):
    assertEquals(project.any.someVal, (project.module1.someVal, project.module2.someVal))

  test("any should combine defs"):
    assertEquals(project.any.someDef, (project.module1.someDef, project.module2.someDef))

  test("any should not include private member"):
    compileErrors("""project.any.privateVal""")

  test("any with non existing field or method should fail compilation"):
    compileErrors("""project.any.nonExisting""")
    compileErrors("project.any.nonExisting()")

  test("any should combine nested submodules"):
    assertEquals(
      project.any.nested.someDefWithAParam("yo"),
      project.module2.nested.someDefWithAParam("yo") *: EmptyTuple
    )

  test("any should not combine submodules"):
    assertEquals(
      project.any.someDefWithAParam("ta"),
      (
        project.module1.someDefWithAParam("ta"),
        project.module2.someDefWithAParam("ta")
      )
    )

  test("all should combine submodules"):
    assertEquals(
      project.all.someDefWithAParam("ta"),
      (
        project.module1.someDefWithAParam("ta"),
        project.module2.someDefWithAParam("ta"),
        project.module2.nested.someDefWithAParam("ta")
      )
    )

  test("all should combine subsubmodules"):
    assertEquals(
      project.all.someDef,
      (
        project.module1.someDef,
        project.module2.someDef,
        project.module2.nested.inception.someDef
      )
    )

  test("all should combine defs with multiple params"):
    assertEquals(
      project.all.someDefWithMultipleParams(1, 2),
      project.module2.someDefWithMultipleParams(1, 2) *: EmptyTuple
    )

  // This does not work because of the Selectable/applyDynamic limitation
  // test("any should combine defs with multiple groups of params"):
  //   assertEquals(
  //     project.any.someDefWithMultipleParams(1, 1)("Result"),
  //     project.module2.someDefWithMultipleParams(1, 1)("Result") *: EmptyTuple
  //   )

  test("all should combine defs with default params"):
    assertEquals(
      project.any.someDefWithDefaultParams(1, Some(0)),
      project.module2.someDefWithDefaultParams(1, Some(0)) *: EmptyTuple
    )

  test("all should combine defs with default params named"):
    assertEquals(
      project.any.someDefWithDefaultParams(1, param3 = Some(0)),
      project.module2.someDefWithDefaultParams(1, param3 = Some(0)) *: EmptyTuple
    )

package wildcardtest:
  object project extends Moulin:
    object module1 extends ScalaModule:
      val someVal                                  = "1"
      def someDef                                  = 2
      def someDefWithAParam(param: String): String = s"module1 $param"

    object module2 extends ScalaModule with MyModule:
      val someVal                                       = 2
      def someDef                                       = "1"
      def someDefWithAParam(param: String) /*: String*/ = s"module2 $param"
      private val privateVal                            = 'm'

      object nested:
        def someDefWithAParam(param: String): String = s"nested $param"

        object inception:
          def someDef: Boolean = false

    private object privateModule:
      val someVal = false
      def someDef = true

    trait MyModule:
      def someDefWithMultipleParams(param1: Int, param2: Int): Boolean = param1 == param2
      def someDefWithDefaultParams(param1: Int, param2: Option[Int] = None, param3: Option[Int] = None): Boolean =
        param2.contains(param1)
