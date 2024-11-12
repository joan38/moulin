package com.goyeau.moulin.cache

import java.nio.file.Files
import java.nio.file.FileSystemException
import java.security.{DigestOutputStream, MessageDigest}
import java.io.{InputStream, OutputStream}
import scala.util.Using
import io.circe.Encoder
import io.circe.Decoder
import os.Path

/** A wrapper around `os.Path` that calculates it's hashcode based on the contents of the filesystem underneath it. Used
  * to ensure filesystem changes can bust caches which are keyed off hashcodes.
  */
case class PathRef(path: os.Path, quick: Boolean, sig: Int)

object PathRef:

  /** Create a [[PathRef]] by recursively digesting the content of a given `path`.
    *
    * @param path
    *   The digested path.
    * @param quick
    *   If `true` the digest is only based to some file attributes (like mtime and size). If `false` the digest is
    *   created of the files content.
    * @return
    */
  def apply(path: Path, quick: Boolean = false): PathRef =
    val basePath = path

    val sig =
      val isPosix   = path.wrapped.getFileSystem.supportedFileAttributeViews().contains("posix")
      val digest    = MessageDigest.getInstance("MD5")
      val digestOut = DigestOutputStream(DummyOutputStream, digest)

      def updateWithInt(value: Int): Unit =
        digest.update((value >>> 24).toByte)
        digest.update((value >>> 16).toByte)
        digest.update((value >>> 8).toByte)
        digest.update(value.toByte)

      if os.exists(path) then
        for (path, attrs) <-
            os.walk.attrs(path, includeTarget = true, followLinks = true).sortBy(_._1.toString)
        do
          val sub = path.subRelativeTo(basePath)
          digest.update(sub.toString().getBytes())
          if !attrs.isDir then
            if isPosix then updateWithInt(os.perms(path, followLinks = false).value)
            if quick then
              val value = (attrs.mtime, attrs.size).hashCode()
              updateWithInt(value)
            else if Files.isReadable(path.toNIO) then
              val is =
                try Some(os.read.inputStream(path))
                catch
                  case _: FileSystemException =>
                    // This is known to happen, when we try to digest a socket file.
                    // We ignore the content of this file for now, as we would do,
                    // when the file isn't readable.
                    // See https://github.com/com-lihaoyi/mill/issues/1875
                    None
              is.foreach {
                Using.resource(_) { is =>
                  StreamSupport.stream(is, digestOut)
                }
              }

      java.util.Arrays.hashCode(digest.digest())

    PathRef(path, quick, sig)

  given Encoder[PathRef] = Encoder.forProduct3("path", "quick", "sig")(pr => (pr.path, pr.quick, pr.sig))
  given Decoder[PathRef] =
    Decoder.forProduct3("path", "quick", "sig")((path: Path, quick: Boolean, sig: Int) => PathRef(path, quick, sig))

object DummyOutputStream extends OutputStream:
  override def write(b: Int): Unit                             = ()
  override def write(b: Array[Byte]): Unit                     = ()
  override def write(b: Array[Byte], off: Int, len: Int): Unit = ()

trait StreamSupport:
  /** Pump the data from the `src` stream into the `dest` stream.
    */
  def stream(src: InputStream, dest: OutputStream): Unit =
    val buffer = new Array[Byte](4096)
    while src.read(buffer) match
        case -1 => false
        case n =>
          dest.write(buffer, 0, n)
          true
    do ()

object StreamSupport extends StreamSupport
