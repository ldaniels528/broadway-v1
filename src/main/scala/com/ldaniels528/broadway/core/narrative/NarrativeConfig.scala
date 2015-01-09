package com.ldaniels528.broadway.core.narrative

import java.io.{File, FilenameFilter}

import com.ldaniels528.broadway.core.location.Location
import com.ldaniels528.broadway.core.resources._
import org.slf4j.LoggerFactory

/**
 * Topology Configuration
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
case class NarrativeConfig(locations: Seq[Location],
                           propertySets: Seq[PropertySet],
                           topologies: Seq[NarrativeDescriptor])


/**
 * Topology Configuration Singleton
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
object NarrativeConfig {
  private[this] lazy val logger = LoggerFactory.getLogger(getClass)

  /**
   * Loads all narrative configurations from the given directory
   * @param directory the given directory
   * @return the collection of successfully parsed [[NarrativeConfig]] objects
   */
  def loadNarrativeConfigs(directory: File): Seq[NarrativeConfig] = {
    logger.info(s"Searching for topology configuration files in '${directory.getAbsolutePath}'...")
    val xmlFile = directory.listFiles(new FilenameFilter {
      override def accept(dir: File, name: String): Boolean = name.toLowerCase.endsWith(".xml")
    })
    xmlFile.toSeq flatMap (f => NarrativeConfigParser.parse(FileResource(f.getAbsolutePath)))
  }

}