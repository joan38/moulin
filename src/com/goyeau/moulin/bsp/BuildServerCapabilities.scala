package com.goyeau.moulin.bsp

import cats.Monoid
import cats.syntax.all.*
import ch.epfl.scala.bsp4j.*
import scala.jdk.CollectionConverters.*

object BuildServerCapabilities:
  given Monoid[BuildServerCapabilities] with
    def empty = new BuildServerCapabilities()
    def combine(x: BuildServerCapabilities, y: BuildServerCapabilities) =
      val capabilities = new BuildServerCapabilities()
      capabilities.setCompileProvider(Option(x.getCompileProvider).combine(Option(y.getCompileProvider)).orNull)
      capabilities.setTestProvider(Option(x.getTestProvider).combine(Option(y.getTestProvider)).orNull)
      capabilities.setRunProvider(Option(x.getRunProvider).combine(Option(y.getRunProvider)).orNull)
      capabilities.setDebugProvider(Option(x.getDebugProvider).combine(Option(y.getDebugProvider)).orNull)
      capabilities.setInverseSourcesProvider(x.getInverseSourcesProvider || y.getInverseSourcesProvider)
      capabilities.setDependencySourcesProvider(x.getDependencySourcesProvider || y.getDependencySourcesProvider)
      capabilities.setDependencyModulesProvider(x.getDependencyModulesProvider || y.getDependencyModulesProvider)
      capabilities.setResourcesProvider(x.getResourcesProvider || y.getResourcesProvider)
      capabilities.setOutputPathsProvider(x.getOutputPathsProvider || y.getOutputPathsProvider)
      capabilities.setBuildTargetChangedProvider(x.getBuildTargetChangedProvider || y.getBuildTargetChangedProvider)
      capabilities.setJvmRunEnvironmentProvider(x.getJvmRunEnvironmentProvider || y.getJvmRunEnvironmentProvider)
      capabilities.setJvmTestEnvironmentProvider(x.getJvmTestEnvironmentProvider || y.getJvmTestEnvironmentProvider)
      capabilities.setCanReload(x.getCanReload || y.getCanReload)
      capabilities

  given Monoid[CompileProvider] with
    def empty = CompileProvider(Seq.empty.asJava)
    def combine(x: CompileProvider, y: CompileProvider) =
      CompileProvider((x.getLanguageIds.asScala ++ y.getLanguageIds.asScala).asJava)

  given Monoid[TestProvider] with
    def empty = TestProvider(Seq.empty.asJava)
    def combine(x: TestProvider, y: TestProvider) =
      TestProvider((x.getLanguageIds.asScala ++ y.getLanguageIds.asScala).asJava)

  given Monoid[RunProvider] with
    def empty = RunProvider(Seq.empty.asJava)
    def combine(x: RunProvider, y: RunProvider) =
      RunProvider((x.getLanguageIds.asScala ++ y.getLanguageIds.asScala).asJava)

  given Monoid[DebugProvider] with
    def empty = DebugProvider(Seq.empty.asJava)
    def combine(x: DebugProvider, y: DebugProvider) =
      DebugProvider((x.getLanguageIds.asScala ++ y.getLanguageIds.asScala).asJava)
