package com.goyeau.moulin.bsp

import cats.syntax.all.*
import ch.epfl.scala.bsp4j.*
import ch.epfl.scala.bsp4j.StatusCode.{ERROR, *}
import com.goyeau.moulin.bsp.BspModule.FullBuildServer
import java.util.concurrent.CompletableFuture
import scala.jdk.CollectionConverters.*
import scala.jdk.FutureConverters.*
import scala.build.bsp.WrappedSourcesParams
import scala.annotation.nowarn
import scala.build.bsp.WrappedSourcesResult
import java.util.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

/** A BuildServer that supplement dependencies info.
  */
class BuildServerWithDeps(buildServer: FullBuildServer, buildServerDependencies: Seq[FullBuildServer])
    extends FullBuildServer:
  override def workspaceBuildTargets(): CompletableFuture[WorkspaceBuildTargetsResult] = (for
    result      <- buildServer.workspaceBuildTargets().asScala
    depsResults <- buildServerDependencies.traverse(_.workspaceBuildTargets().asScala)
    (depsTargetIdsTest, depsTargetIds) = depsResults
      .flatMap(_.getTargets.asScala)
      .map(_.getId)
      .partition(_.getUri.endsWith("-test"))
    targets = result.getTargets.asScala.map(target =>
      val moulinDeps = if target.getId.getUri.endsWith("-test") then depsTargetIdsTest else depsTargetIds
      target.setDependencies((target.getDependencies.asScala ++ moulinDeps).asJava)
      target
    )
  yield WorkspaceBuildTargetsResult(targets.asJava)).asJava.toCompletableFuture

  override def buildTargetDependencySources(
      params: DependencySourcesParams
  ): CompletableFuture[DependencySourcesResult] =
    buildServer.buildTargetDependencySources(params)

  override def buildTargetInverseSources(params: InverseSourcesParams): CompletableFuture[InverseSourcesResult] =
    buildServer.buildTargetInverseSources(params)

  override def buildTargetSources(params: SourcesParams): CompletableFuture[SourcesResult] =
    buildServer.buildTargetSources(params)

  override def buildTargetResources(params: ResourcesParams): CompletableFuture[ResourcesResult] =
    buildServer.buildTargetResources(params)

  override def buildTargetDependencyModules(
      params: DependencyModulesParams
  ): CompletableFuture[DependencyModulesResult] =
    buildServer.buildTargetDependencyModules(params)

  override def buildTargetTest(params: TestParams): CompletableFuture[TestResult] =
    buildServer.buildTargetTest(params)

  override def buildTargetRun(params: RunParams): CompletableFuture[RunResult] =
    buildServer.buildTargetRun(params)

  override def buildTargetOutputPaths(params: OutputPathsParams): CompletableFuture[OutputPathsResult] =
    buildServer.buildTargetOutputPaths(params)

  override def buildTargetCleanCache(params: CleanCacheParams): CompletableFuture[CleanCacheResult] =
    buildServer.buildTargetCleanCache(params)

  override def onBuildExit(): Unit =
    buildServer.onBuildExit()

  override def buildTargetCompile(params: CompileParams): CompletableFuture[CompileResult] =
    buildServer.buildTargetCompile(params)

  override def debugSessionStart(params: DebugSessionParams): CompletableFuture[DebugSessionAddress] =
    buildServer.debugSessionStart(params)

  override def onBuildInitialized(): Unit =
    buildServer.onBuildInitialized()

  override def buildInitialize(params: InitializeBuildParams): CompletableFuture[InitializeBuildResult] =
    buildServer.buildInitialize(params)

  override def buildShutdown(): CompletableFuture[Object] =
    buildServer.buildShutdown()

  override def workspaceReload(): CompletableFuture[Object] =
    buildServer.workspaceReload()

  override def buildTargetJavacOptions(params: JavacOptionsParams): CompletableFuture[JavacOptionsResult] =
    buildServer.buildTargetJavacOptions(params)

  override def buildTargetJvmRunEnvironment(
      params: JvmRunEnvironmentParams
  ): CompletableFuture[JvmRunEnvironmentResult] =
    buildServer.buildTargetJvmRunEnvironment(params)

  override def buildTargetJvmTestEnvironment(
      params: JvmTestEnvironmentParams
  ): CompletableFuture[JvmTestEnvironmentResult] =
    buildServer.buildTargetJvmTestEnvironment(params)

  @nowarn
  override def buildTargetScalaTestClasses(params: ScalaTestClassesParams): CompletableFuture[ScalaTestClassesResult] =
    buildServer.buildTargetScalaTestClasses(params)

  @nowarn
  override def buildTargetScalaMainClasses(params: ScalaMainClassesParams): CompletableFuture[ScalaMainClassesResult] =
    buildServer.buildTargetScalaMainClasses(params)

  override def buildTargetScalacOptions(params: ScalacOptionsParams): CompletableFuture[ScalacOptionsResult] =
    buildServer.buildTargetScalacOptions(params)

  override def buildTargetWrappedSources(params: WrappedSourcesParams): CompletableFuture[WrappedSourcesResult] =
    buildServer.buildTargetWrappedSources(params)
