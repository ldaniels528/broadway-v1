package com.github.ldaniels528.broadway.core.io.trigger

import com.github.ldaniels528.broadway.core.io.archive.FileArchive

/**
  * Represents a Directory which may contain file files
  */
case class FileFeedDirectory(path: String, feeds: Seq[FileFeed], archive: Option[FileArchive])