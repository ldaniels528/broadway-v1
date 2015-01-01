package com.ldaniels528.broadway.core.topology

import java.util.Properties

import com.ldaniels528.broadway.core.Resources.ReadableResource
import com.ldaniels528.broadway.core.util.XMLHelper._
import com.ldaniels528.trifecta.util.OptionHelper._
import com.ldaniels528.trifecta.util.PropertiesHelper._

import scala.xml.{Node, XML}

/**
 * Topology Configuration Parser Singleton
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
object TopologyConfigParser {

  /**
   * Parses the given resource and returns an option of a topology configuration
   * @param resource the given [[ReadableResource]]
   * @return an option of a [[TopologyConfig]]
   */
  def parse(resource: ReadableResource): Option[TopologyConfig] = {
    resource.getInputStream map { in =>
      val doc = XML.load(in)
      val topologies = parseTopologies(doc)
      new TopologyConfig(
        locations = parseLocations(topologies, doc),
        propertySets = parsePropertiesRef(doc),
        topologies)
    }
  }

  /**
   * Parses the <code>feed</code> tags
   * @param doc the given XML node
   * @return a [[Seq]] of [[FeedDescriptor]]
   */
  private def parseFeeds(topologies: Seq[TopologyDescriptor], doc: Node) = {
    val tagName = "feed"
    (doc \ tagName) map { node =>
      val name = node.getAttr(tagName, "name")
      val matching = node.getAttr(tagName, "match")
      val refId_? = node.getAttrOpt("topology-ref")
      val topology_? = refId_?.map(refId => topologies.find(_.id == refId).orDie(s"Topology '$refId' not found"))
      FeedDescriptor(name, matching, topology = topology_?)
    }
  }

  /**
   * Parses the <code>location</code> tags
   * @param doc the given XML node
   * @return a [[Seq]] of [[Location]]
   */
  private def parseLocations(topologies: Seq[TopologyDescriptor], doc: Node) = {
    val tagName = "location"
    (doc \ tagName) map { node =>
      Location(
        id = node.getAttr(tagName, "id"),
        path = node.getAttr(tagName, "path"),
        feeds = parseFeeds(topologies, node))
    }
  }

  /**
   * Parses the <code>property</code> tags
   * @param doc the given XML node
   * @return a [[Seq]] of [[PropertySet]]
   */
  private def parseProperty(doc: Node): Seq[(String, String)] = {
    val tagName = "property"
    (doc \ tagName) map { node =>
      (node.getAttr(tagName, "key"), node.getAttr(tagName, "value"))
    }
  }

  /**
   * Parses the <code>properties</code> tags
   * @param doc the given XML node
   * @return a [[Seq]] of [[java.util.Properties]]
   */
  private def parseProperties(doc: Node) = {
    val tagName = "properties"
    (doc \ tagName) map { node =>
      val id = node.getAttr(tagName, "id")
      val props = Map(parseProperty(node): _*).toProps
      PropertySet(id, props)
    }
  }

  /**
   * Parses the <code>properties-ref</code> tags
   * @param doc the given XML node
   * @return a [[Seq]] of [[PropertySet]]
   */
  private def parsePropertiesRef(doc: Node) = {
    val tagName = "properties-ref"
    (doc \ tagName) map { node =>
      val id = node.getAttr(tagName, "id")
      val props = Map(parseProperty(node): _*).toProps
      PropertySet(id, props)
    }
  }

  /**
   * Parses the <code>topology</code> tags
   * @param doc the given XML node
   * @return a [[Seq]] of [[TopologyDescriptor]]
   */
  private def parseTopologies(doc: Node) = {
    val tagName = "topology"
    (doc \ tagName) map { node =>
      val id = node.getAttr(tagName, "id")
      val className = node.getAttr(tagName, "class")
      TopologyDescriptor(id, className, getTopologyPropertiesRef(node))
    }
  }

  private def getTopologyPropertiesRef(node: Node): TopologyRuntime => Properties = {
    { rt =>
      node.getAttrOpt("properties-ref") match {
        case Some(refId) => rt.getPropertiesByID(refId).orDie(s"A properties set for '$refId' could not be found")
        case None => Map(parseProperty(node): _*).toProps
      }
    }
  }

}
