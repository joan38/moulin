package com.goyeau.moulin.bsp

import ch.epfl.scala.bsp4j.*

class MoulinBuildClient extends BuildClient:
  private var client: BuildClient             = null
  def connect(scalaClient: BuildClient): Unit = client = scalaClient

  def onBuildLogMessage(params: LogMessageParams): Unit                 = client.onBuildLogMessage(params)
  def onBuildPublishDiagnostics(params: PublishDiagnosticsParams): Unit = client.onBuildPublishDiagnostics(params)
  def onBuildShowMessage(params: ShowMessageParams): Unit               = client.onBuildShowMessage(params)
  def onBuildTargetDidChange(params: DidChangeBuildTarget): Unit        = client.onBuildTargetDidChange(params)
  def onBuildTaskFinish(params: TaskFinishParams): Unit                 = client.onBuildTaskFinish(params)
  def onBuildTaskProgress(params: TaskProgressParams): Unit             = client.onBuildTaskProgress(params)
  def onBuildTaskStart(params: TaskStartParams): Unit                   = client.onBuildTaskStart(params)
