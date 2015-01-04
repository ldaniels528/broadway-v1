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

  def getFeed(fd: FeedDescriptor): Feed = {
    feeds.getOrElseUpdate(fd.uuid, Feed(fd.uuid, fd.name, fd.dependencies map (_.toFeed(this)), fd.narrative))
  }

  def getPropertiesByID(id: String): Option[Properties] = properties.get(id)

  def getNarrative(config: ServerConfig, td: NarrativeDescriptor): Try[BroadwayNarrative] = Try {
    narratives.getOrElseUpdate(td.id, instantiateTopology(config, td.className))
  }

  def getNarrativeByID(id: String): BroadwayNarrative = {
    narratives.get(id) orDie s"Topology ID '$id' not found"
  }

  private def instantiateTopology(config: ServerConfig, className: String) = {
    val topologyClass = Class.forName(className)
    val cons = topologyClass.getConstructors()(0)
    cons.newInstance(config).asInstanceOf[BroadwayNarrative]
  }

}
