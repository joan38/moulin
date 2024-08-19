package com.goyeau.moulin.bsp

import java.io.InputStream
import java.io.OutputStream

class LoggedInputStream(inputStream: InputStream, outputStream: OutputStream) extends InputStream:
  override def read(): Int =
    val byte = inputStream.read()
    outputStream.write(byte)
    byte
