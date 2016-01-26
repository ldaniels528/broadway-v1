package com.github.ldaniels528.broadway.app.config

import java.io.{File, FilenameFilter}

import org.slf4j.LoggerFactory

/**
  * Server Config
  * http://hubpages.com/entertainment/Broadway-and-Theater-Vocabulary-and-Terms
  *
  * @author lawrence.daniels@gmail.com
  */
case class ServerConfig(directories: Directories) {
  private lazy val logger = LoggerFactory.getLogger(getClass)

  def init() = {
    loadAnthologies()
  }

  /**
    * Loads all stories from the configured directory
    *
    * @return the collection of successfully parsed [[Story story]] instances
    */
  private def loadAnthologies(): Seq[Story] = {
    logger.info(s"Searching for story configuration files in '${directories.base.getAbsolutePath}'...")
    val storyFiles = directories.stories.listFiles(new FilenameFilter {
      override def accept(dir: File, name: String): Boolean = name.toLowerCase.endsWith(".xml")
    })
    storyFiles map StoryParser.parse
  }

}
