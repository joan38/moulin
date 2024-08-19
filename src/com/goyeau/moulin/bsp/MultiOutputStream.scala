package com.goyeau.moulin.bsp

import java.io.OutputStream

class MultiOutputStream(outputStreams: OutputStream*) extends OutputStream:
  override def write(b: Int)                             = outputStreams.foreach(_.write(b))
  override def write(b: Array[Byte])                     = outputStreams.foreach(_.write(b))
  override def write(b: Array[Byte], off: Int, len: Int) = outputStreams.foreach(_.write(b, off, len))
  override def flush()                                   = outputStreams.foreach(_.flush())
  override def close()                                   = outputStreams.foreach(_.close())
