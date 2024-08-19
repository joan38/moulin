package com.goyeau.moulin.cache

import munit.FunSuite
import com.goyeau.moulin.cache.Cache.cached
import cachetest.*

class CacheTest extends FunSuite:
  given Cache = new Cache:
    val path = os.temp()

  test("cached should cache"):
    val sourceFile = project.someSourceFile
    os.write.over(sourceFile, "v0")

    assertEquals(project.compileRuns, 0)
    assertEquals(project.compile(), "v0")
    assertEquals(project.compileRuns, 1)
    assertEquals(project.compile(), "v0")
    assertEquals(project.compileRuns, 1)
    os.write.over(sourceFile, "v1")

    assertEquals(project.compile(), "v1")
    assertEquals(project.compileRuns, 2)

package cachetest:
  object project:
    var compileRuns    = 0
    val someSourceFile = os.temp()

    def sources = Seq(PathRef(someSourceFile))

    def compile() = cached(sources): sources =>
      compileRuns += 1
      sources.map(pathRef => os.read(pathRef.path)).mkString("\n")
