package com.goyeau.moulin.util

object Scala:
  val ReleaseVersion       = raw"""(\d+)\.(\d+)\.(\d+)""".r
  val MinorSnapshotVersion = raw"""(\d+)\.(\d+)\.([1-9]\d*)-SNAPSHOT""".r
  val DottyVersion         = raw"""0\.(\d+)\.(\d+).*""".r
  val Scala3EarlyVersion   = raw"""3\.0\.0-(\w+).*""".r
  val Scala3Version        = raw"""3\.(\d+)\.(\d+).*""".r
  val NightlyVersion       = raw"""(\d+)\.(\d+)\.(\d+)-bin-[a-f0-9]*""".r
  val TypelevelVersion     = raw"""(\d+)\.(\d+)\.(\d+)-bin-typelevel.*""".r

  /** Returns the Scala binary version for the given Scala version.
    */
  def scalaBinaryVersion(scalaVersion: String) = scalaVersion match
    case Scala3EarlyVersion(milestone)         => s"3.0.0-$milestone"
    case Scala3Version(_, _)                   => "3"
    case ReleaseVersion(major, minor, _)       => s"$major.$minor"
    case MinorSnapshotVersion(major, minor, _) => s"$major.$minor"
    case NightlyVersion(major, minor, _)       => s"$major.$minor"
    case DottyVersion(minor, _)                => s"0.$minor"
    case TypelevelVersion(major, minor, _)     => s"$major.$minor"
    case _                                     => scalaVersion
