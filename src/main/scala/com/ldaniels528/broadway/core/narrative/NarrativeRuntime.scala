package com.ldaniels528.broadway.core.narrative

import java.util.Properties

import com.ldaniels528.broadway.BroadwayNarrative
import com.ldaniels528.broadway.server.ServerConfig
import com.ldaniels528.trifecta.util.OptionHelper._

import scala.collection.concurrent.TrieMap
import scala.util.Try

/**
 * Represents a narrative runtime
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
class NarrativeRuntime() {
  private val feeds = TrieMap[String, Feed]()
  private val narratives = TrieMap[String, BroadwayNarrative]()
  private val properties = TrieMap[String, Properties]()

  /**
   * Creates or retrieves a feed based on the given descriptor
   * @param fd the given [[FeedDescriptor]]
   * @return the [[Feed]]
   */
  def getFeed(fd: FeedDescriptor): Feed = {
    feeds.getOrElseUpdate(fd.uuid, Feed(fd.uuid, fd.name, fd.dependencies map (_.toFeed(this)), fd.narrative))
  }

  /**
   * Retrieves a set of properties by ID
   * @param id the given property set ID
   * @return an option of a set of [[Properties]]
   */
  def getPropertiesByID(id: String): Option[Properties] = properties.get(id)

  /**
   * Retrieves or creates a Broadway narrative instance
   * @param config the given [[ServerConfig]]
   * @param td the given [[NarrativeDescriptor]]
   * @return an outcome of a [[BroadwayNarrative]]
   */
  def getNarrative(config: ServerConfig, td: NarrativeDescriptor): Try[BroadwayNarrative] = Try {
    narratives.getOrElseUpdate(td.id, instantiateTopology(config, td.id, td.className, td.properties))
  }

  /**
   * Retrieves a narrative by its unique identifier
   * @param id the given unique identifier
   * @return the [[BroadwayNarrative]]
   */
  def getNarrativeByID(id: String): BroadwayNarrative = {
    narratives.get(id) orDie s"Narrative ID '$id' not found"
  }

  /**
   * Creates an instance of a Broadway Narrative
   * @param config the given [[ServerConfig]]
   * @param className the given class name
   * @return the [[BroadwayNarrative]]
   */
  private def instantiateTopology(config: ServerConfig, id: String, className: String, props: Properties) = {
    val topologyClass = Class.forName(className)
    val cons = topologyClass.getConstructors()(0)
    cons.newInstance(config, id, props).asInstanceOf[BroadwayNarrative]
  }

}
