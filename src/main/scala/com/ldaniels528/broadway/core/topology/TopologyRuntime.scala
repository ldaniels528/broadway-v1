package com.ldaniels528.broadway.core.topology

import java.util.Properties

import com.ldaniels528.broadway.BroadwayNarrative
import com.ldaniels528.broadway.server.ServerConfig
import com.ldaniels528.trifecta.util.OptionHelper._

import scala.collection.concurrent.TrieMap
import scala.util.Try

/**
 * Represents a topology runtime
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
class TopologyRuntime() {
  private val feeds = TrieMap[String, Feed]()
  private val topologies = TrieMap[String, BroadwayNarrative]()
  private val properties = TrieMap[String, Properties]()

  def getFeed(fd: FeedDescriptor): Feed = {
    feeds.getOrElseUpdate(fd.uuid, Feed(fd.uuid, fd.name, fd.dependencies map (_.toFeed(this)), fd.topology))
  }

  def getPropertiesByID(id: String): Option[Properties] = properties.get(id)

  def getTopology(config: ServerConfig, td: TopologyDescriptor): Try[BroadwayNarrative] = Try {
    topologies.getOrElseUpdate(td.id, instantiateTopology(config, td.className))
  }

  def getTopologyByID(id: String): BroadwayNarrative = {
    topologies.get(id) orDie s"Topology ID '$id' not found"
  }

  private def instantiateTopology(config: ServerConfig, className: String) = {
    val topologyClass = Class.forName(className)
    val cons = topologyClass.getConstructors()(0)
    cons.newInstance(config).asInstanceOf[BroadwayNarrative]
  }

}
