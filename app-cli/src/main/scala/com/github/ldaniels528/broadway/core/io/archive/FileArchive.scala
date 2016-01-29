package com.github.ldaniels528.broadway.core.io.archive

import java.io.File

import com.github.ldaniels528.broadway.core.io.archive.CompressionTypes.CompressionType

/**
  * Represents a file archive
  */
case class FileArchive(id: String, base: File, compressionType: CompressionType = CompressionTypes.NONE)