package com.github.ldaniels528.broadway.core.io.trigger.impl

import java.io.File

import com.github.ldaniels528.broadway.core.io.archive.Archive

/**
  * Represents a Directory which may contain file files
  * @author lawrence.daniels@gmail.com
  */
case class FileFeedDirectory(path: String, feeds: Seq[FileFeed], archive: Option[Archive])
  extends FeedContainer {

  override def find(file: File) = feeds.find(_.matches(file))

}