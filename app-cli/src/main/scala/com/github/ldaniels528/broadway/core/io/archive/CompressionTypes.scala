package com.github.ldaniels528.broadway.core.io.archive

/**
  * An enumeration of Compression Types
  */
object CompressionTypes extends Enumeration {
  type CompressionType = Value

  val NONE, ZIP, GZIP = Value

}
