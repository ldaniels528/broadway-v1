package com.ldaniels528.broadway.core.topology

import java.io.{File, FilenameFilter}

import com.ldaniels528.broadway.core.Resources.FileResource
import org.slf4j.LoggerFactory

/**
 * Topology Configuration
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
case class TopologyConfig(locations: Seq[Location],
                          propertySets: Seq[PropertySet],
                          topologies: Seq[TopologyDescriptor])


/**
 * Topology Configuration Singleton
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
object TopologyConfig {
  private[this] lazy val logger = LoggerFactory.getLogger(getClass)

  /**
   * Loads all topology configurations from the given directory
   * @param directory the given directory
   * @return the collection of successfully parsed [[TopologyConfig]] objects
   */
  def loadTopologyConfigs(directory: File): Seq[TopologyConfig] = {
    logger.info(s"Searching for topology configuration files in '${directory.getAbsolutePath}'...")
    val xmlFile = directory.listFiles(new FilenameFilter {
      override def accept(dir: File, name: String): Boolean = name.toLowerCase.endsWith(".xml")
    })
    xmlFile.toSeq flatMap (f => TopologyConfigParser.parse(FileResource(f.getAbsolutePath)))
  }

}