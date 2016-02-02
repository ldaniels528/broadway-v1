package com.github.ldaniels528.broadway.core.io.archive.impl

import com.github.ldaniels528.broadway.core.io.archive.CompressionTypes._
import com.github.ldaniels528.broadway.core.io.archive.{Archive, CompressionTypes}

/**
  * Represents a file archive
  * @author lawrence.daniels@gmail.com
  */
case class FileArchive(id: String, basePath: String, compressionType: CompressionType = CompressionTypes.NONE) extends Archive