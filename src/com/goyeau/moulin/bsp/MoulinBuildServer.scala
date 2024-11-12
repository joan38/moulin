package com.goyeau.moulin.bsp

import annotation.nowarn
import cats.implicits.*
import ch.epfl.scala.bsp4j.*
import ch.epfl.scala.bsp4j.StatusCode.{ERROR, *}
import com.goyeau.moulin.Moulin
import com.goyeau.moulin.bsp.BuildServerCapabilities.given
import com.goyeau.moulin.bsp.BspModule.FullBuildServer
import java.util.concurrent.CompletableFuture
import scala.build.bsp.WrappedSourcesParams
import scala.build.bsp.WrappedSourcesResult
import scala.jdk.CollectionConverters.*
import scala.jdk.FutureConverters.*
import scala.concurrent.ExecutionContext.Implicits.global

class MoulinBuildServer(scalaServers: Seq[FullBuildServer]) extends FullBuildServer:
  def buildInitialize(params: InitializeBuildParams): CompletableFuture[InitializeBuildResult] =
    (for results <- scalaServers.traverse(_.buildInitialize(params).asScala)
    yield InitializeBuildResult(
      Moulin.name,
      Moulin.version,
      Bsp4j.PROTOCOL_VERSION,
      results.map(_.getCapabilities).combineAll
    )).asJava.toCompletableFuture

  def buildTargetCleanCache(params: CleanCacheParams): CompletableFuture[CleanCacheResult] =
    forwardRequestToScala(
      params.getTargets,
      CleanCacheParams(_),
      _.buildTargetCleanCache(_),
      results => CleanCacheResult(results.forall(_.getCleaned))
    )

  def buildTargetCompile(params: CompileParams): CompletableFuture[CompileResult] =
    forwardRequestToScala(
      params.getTargets,
      CompileParams(_),
      _.buildTargetCompile(_),
      results =>
        CompileResult(
          results
            .map(_.getStatusCode)
            .reduce:
              case (ERROR, _)     => ERROR
              case (_, ERROR)     => ERROR
              case (CANCELLED, _) => ERROR
              case (_, CANCELLED) => ERROR
              case _              => OK
        )
    )

  def buildTargetDependencySources(params: DependencySourcesParams): CompletableFuture[DependencySourcesResult] =
    forwardRequestToScala(
      params.getTargets,
      DependencySourcesParams(_),
      _.buildTargetDependencySources(_),
      results => DependencySourcesResult(results.flatMap(_.getItems.asScala).asJava)
    )

  def buildTargetDependencyModules(params: DependencyModulesParams): CompletableFuture[DependencyModulesResult] =
    forwardRequestToScala(
      params.getTargets,
      DependencyModulesParams(_),
      _.buildTargetDependencyModules(_),
      results => DependencyModulesResult(results.flatMap(_.getItems.asScala).asJava)
    )

  def buildTargetInverseSources(params: InverseSourcesParams): CompletableFuture[InverseSourcesResult] = (for
    results <- scalaServers.traverse(_.buildTargetInverseSources(params).asScala)
    targets = results.flatMap(_.getTargets.asScala)
  yield InverseSourcesResult(targets.asJava)).asJava.toCompletableFuture

  def buildTargetResources(params: ResourcesParams): CompletableFuture[ResourcesResult] =
    forwardRequestToScala(
      params.getTargets,
      ResourcesParams(_),
      _.buildTargetResources(_),
      results => ResourcesResult(results.flatMap(_.getItems.asScala).asJava)
    )

  def buildTargetOutputPaths(params: OutputPathsParams): CompletableFuture[OutputPathsResult] =
    forwardRequestToScala(
      params.getTargets,
      OutputPathsParams(_),
      _.buildTargetOutputPaths(_),
      results => OutputPathsResult(results.flatMap(_.getItems.asScala).asJava)
    )

  def buildTargetRun(params: RunParams): CompletableFuture[RunResult] =
    forwardRequestToScala(
      Seq(params.getTarget).asJava,
      targets => RunParams(targets.asScala.head),
      _.buildTargetRun(_),
      results =>
        RunResult(
          results
            .map(_.getStatusCode)
            .reduce:
              case (ERROR, _)     => ERROR
              case (_, ERROR)     => ERROR
              case (CANCELLED, _) => ERROR
              case (_, CANCELLED) => ERROR
              case _              => OK
        )
    )

  def buildTargetSources(params: SourcesParams): CompletableFuture[SourcesResult] =
    forwardRequestToScala(
      params.getTargets,
      SourcesParams(_),
      _.buildTargetSources(_),
      results => SourcesResult(results.flatMap(_.getItems.asScala).asJava)
    )

  def buildTargetTest(params: TestParams): CompletableFuture[TestResult] =
    forwardRequestToScala(
      params.getTargets,
      TestParams(_),
      _.buildTargetTest(_),
      results =>
        TestResult(
          results
            .map(_.getStatusCode)
            .reduce:
              case (ERROR, _)     => ERROR
              case (_, ERROR)     => ERROR
              case (CANCELLED, _) => ERROR
              case (_, CANCELLED) => ERROR
              case _              => OK
        )
    )

  def buildTargetWrappedSources(params: WrappedSourcesParams): CompletableFuture[WrappedSourcesResult] =
    forwardRequestToScala(
      params.getTargets,
      WrappedSourcesParams(_),
      _.buildTargetWrappedSources(_),
      results => WrappedSourcesResult(results.flatMap(_.getItems.asScala).asJava)
    )

  def buildShutdown(): CompletableFuture[Object] = (for _ <- scalaServers.traverse(_.buildShutdown().asScala)
  yield Object()).asJava.toCompletableFuture

  def debugSessionStart(params: DebugSessionParams): CompletableFuture[DebugSessionAddress] = ???

  def onBuildInitialized(): Unit = scalaServers.foreach(_.onBuildInitialized())

  def onBuildExit(): Unit = scalaServers.foreach(_.onBuildExit())

  def workspaceBuildTargets(): CompletableFuture[WorkspaceBuildTargetsResult] = (for
    results <- scalaServers.traverse(_.workspaceBuildTargets().asScala)
    targets = results.flatMap(_.getTargets.asScala)
  yield WorkspaceBuildTargetsResult(targets.asJava)).asJava.toCompletableFuture

  def workspaceReload(): CompletableFuture[Object] = (for _ <- scalaServers.traverse(_.workspaceReload().asScala)
  yield Object()).asJava.toCompletableFuture

  def buildTargetScalacOptions(params: ScalacOptionsParams): CompletableFuture[ScalacOptionsResult] =
    forwardRequestToScala(
      params.getTargets,
      ScalacOptionsParams(_),
      _.buildTargetScalacOptions(_),
      results => ScalacOptionsResult(results.flatMap(_.getItems.asScala).asJava)
    )

  @nowarn
  def buildTargetScalaTestClasses(params: ScalaTestClassesParams): CompletableFuture[ScalaTestClassesResult] =
    forwardRequestToScala(
      params.getTargets,
      ScalaTestClassesParams(_),
      _.buildTargetScalaTestClasses(_),
      results => ScalaTestClassesResult(results.flatMap(_.getItems.asScala).asJava)
    )

  @nowarn
  def buildTargetScalaMainClasses(params: ScalaMainClassesParams): CompletableFuture[ScalaMainClassesResult] =
    forwardRequestToScala(
      params.getTargets,
      ScalaMainClassesParams(_),
      _.buildTargetScalaMainClasses(_),
      results => ScalaMainClassesResult(results.flatMap(_.getItems.asScala).asJava)
    )

  def buildTargetJavacOptions(params: JavacOptionsParams): CompletableFuture[JavacOptionsResult] =
    forwardRequestToScala(
      params.getTargets,
      JavacOptionsParams(_),
      _.buildTargetJavacOptions(_),
      results => JavacOptionsResult(results.flatMap(_.getItems.asScala).asJava)
    )

  def buildTargetJvmTestEnvironment(params: JvmTestEnvironmentParams): CompletableFuture[JvmTestEnvironmentResult] =
    forwardRequestToScala(
      params.getTargets,
      JvmTestEnvironmentParams(_),
      _.buildTargetJvmTestEnvironment(_),
      results => JvmTestEnvironmentResult(results.flatMap(_.getItems.asScala).asJava)
    )

  def buildTargetJvmRunEnvironment(params: JvmRunEnvironmentParams): CompletableFuture[JvmRunEnvironmentResult] =
    forwardRequestToScala(
      params.getTargets,
      JvmRunEnvironmentParams(_),
      _.buildTargetJvmRunEnvironment(_),
      results => JvmRunEnvironmentResult(results.flatMap(_.getItems.asScala).asJava)
    )

  private def forwardRequestToScala[Param, Result, Item](
      getTargets: => java.util.List[BuildTargetIdentifier],
      createParam: java.util.List[BuildTargetIdentifier] => Param,
      callRequest: (FullBuildServer, Param) => CompletableFuture[Result],
      combineResults: Seq[Result] => Result
  ): CompletableFuture[Result] = (for results <- scalaServers.traverse(server =>
      for
        targetResult <- server.workspaceBuildTargets().asScala
        supportedTargets = getTargets.asScala.intersect(targetResult.getTargets.asScala.map(_.getId))
        result <- callRequest(server, createParam(supportedTargets.asJava)).asScala
      yield result
    )
  yield combineResults(results)).asJava.toCompletableFuture
end MoulinBuildServer
