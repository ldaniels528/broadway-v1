package com.github.ldaniels528.broadway.core.io.archival

import java.io.File

import com.github.ldaniels528.broadway.core.io.archival.CompressionTypes.CompressionType

/**
  * Represents a file archival strategy
  */
case class Archive(id: String, base: File, compressionType: CompressionType = CompressionTypes.NONE)