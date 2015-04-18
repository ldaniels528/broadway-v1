package com.ldaniels528.broadway.core.narrative

import java.util.Properties

import com.ldaniels528.broadway.core.resources._
import com.ldaniels528.broadway.core.triggers.Trigger
import com.ldaniels528.broadway.core.triggers.location.{FileLocation, HttpLocation, Location}
import com.ldaniels528.broadway.core.triggers.schedules.Scheduling
import com.ldaniels528.broadway.core.util.XMLHelper._
import com.ldaniels528.broadway.server.ServerConfig.HttpInfo
import com.ldaniels528.commons.helpers.OptionHelper._
import com.ldaniels528.commons.helpers.PropertiesHelper._

import scala.util.{Failure, Success, Try}
import scala.xml.{Node, XML}

/**
 * Anthology Configuration Parser
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
object AnthologyParser {

  /**
   * Parses the given resource and returns an option of a narrative configuration
   * @param resource the given [[ReadableResource]]
   * @return an option of a [[Anthology]]
   */
  def parse(resource: ReadableResource): Option[Anthology] = {
    resource.getInputStream map { in =>
      val doc = XML.load(in)
      val id = doc.getAttr("anthology", "id")
      val narratives = parseNarratives(doc)
      val schedules = parseSchedules(doc)
      val resources = parseResources(doc)
      val triggers = parseTriggers(doc, narratives, schedules, resources)

      new Anthology(
        id,
        locations = parseLocations(narratives, doc),
        propertySets = parseNamedPropertiesRef(doc),
        schedules,
        narratives,
        triggers)
    }
  }

  /**
   * Creates a new class instance
   * @param id the given object identifier
   * @param className the given class name for which to instantiate
   * @return the instantiated class
   */
  private def instantiate(id: String, className: String) = Try {
    val `class` = Class.forName(className)
    val constructor = `class`.getConstructor(classOf[String])
    constructor.newInstance(id)
  }

  /**
   * Parses the <code>directories</code> tag
   * @param doc the given XML node
   * @return a [[java.util.Properties]] object containing the specified properties
   */
  private def parseDirectories(doc: Node) = {
    val fields = Seq("archive", "base", "completed", "failed", "incoming", "topologies", "work")
    val mapping = (doc \ "directories") flatMap { node =>
      fields flatMap (field => node.getText(field) map (value => (s"broadway.directories.$field", value)))
    }
    Map(mapping: _*).toProps
  }

  /**
   * Parses the <code>feed</code> tags
   * @param doc the given XML node
   * @return a [[Seq]] of [[FeedDescriptor]]
   */
  private def parseFeeds(topologies: Seq[NarrativeDescriptor], doc: Node) = {
    val tagName = "feed"
    (doc \ tagName) map { node =>
      val name = node.getAttr(tagName, "name")
      val matching = node.getAttr(tagName, "match")
      val refId_? = node.getAttrOpt("narrative-ref")
      val topology_? = refId_?.map(refId => topologies.find(_.id == refId).orDie(s"Topology '$refId' not found"))
      FeedDescriptor(name, matching, narrative = topology_?)
    }
  }

  /**
   * Parses the <code>http</code> tag
   * @param doc the given XML node
   * @return an [[Option]] of a [[HttpInfo]]
   */
  private def parseHttpInfo(doc: Node) = {
    ((doc \ "http") map { node =>
      val host = (node \ "host").text
      val port = (node \ "port").text
      HttpInfo(host, port.toInt)
    }).headOption
  }

  /**
   * Parses the <code>location</code> tags
   * @param doc the given XML node
   * @return a [[Seq]] of [[Location]]
   */
  private def parseLocations(topologies: Seq[NarrativeDescriptor], doc: Node): Seq[Location] = {
    val tagName = "location"
    (doc \ tagName) map { node =>
      // get the ID, path and feeds
      val id = node.getAttr(tagName, "id")
      val path = node.getAttr(tagName, "path")
      val feeds = parseFeeds(topologies, node)

      path match {
        case url if url.startsWith("http:") => HttpLocation(id, url, feeds)
        case _ => FileLocation(id, path, feeds)
      }
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
      (node.getAttr(tagName, "key"), node.text)
    }
  }

  /**
   * Parses the <code>properties</code> tags
   * @param doc the given XML node
   * @return a [[Seq]] of [[java.util.Properties]]
   */
  private def parseProperties(doc: Node) = {
    Map((doc \ "properties") flatMap parseProperty: _*).toProps
  }

  /**
   * Parses the <code>properties</code> tags
   * @param doc the given XML node
   * @return a [[Seq]] of [[java.util.Properties]]
   */
  private def parseNamedProperties(doc: Node) = {
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
  private def parseNamedPropertiesRef(doc: Node) = {
    val tagName = "properties-ref"
    (doc \ tagName) map { node =>
      val id = node.getAttr(tagName, "id")
      val props = Map(parseProperty(node): _*).toProps
      PropertySet(id, props)
    }
  }

  private def parsePropertiesRef(node: Node): NarrativeRuntime => Properties = {
    { rt =>
      node.getAttrOpt("properties-ref") match {
        case Some(refId) => rt.getPropertiesByID(refId).orDie(s"A properties set for '$refId' could not be found")
        case None => Map(parseProperty(node): _*).toProps
      }
    }
  }

  private def parseResources(doc: Node) = {
    val tagName = "resource"
    (doc \ tagName) map { node =>
      val id = node.getAttr(tagName, "id")
      val className = node.getAttr(tagName, "class")

      instantiate(id, className) match {
        case Success(resource: Resource) => UserResource(id, resource)
        case Success(unknown) =>
          throw new IllegalStateException(s"Resource '$id' (class $className) does not implement ${classOf[Resource].getName}")
        case Failure(e) =>
          throw new IllegalStateException(s"Failed to load resource '$id' (class $className): ${e.getMessage}")
      }
    }
  }

  /**
   * Parses the <code>schedule</code> tag
   * @param doc the given XML node
   * @return a collection of [[Scheduling]] objects
   */
  private def parseSchedules(doc: Node) = {
    val tagName = "schedule"
    (doc \ tagName) map { node =>
      val id = node.getAttr(tagName, "id")
      val className = node.getAttr(tagName, "class")

      instantiate(id, className) match {
        case Success(schedule: Scheduling) => schedule
        case Success(unknown) =>
          throw new IllegalStateException(s"Schedule '$id' (class $className}) does not implement ${classOf[Scheduling].getName}")
        case Failure(e) =>
          throw new IllegalStateException(s"Failed to load schedule '$id' (class $className})")
      }
    }
  }

  /**
   * Parses the <code>narrative</code> tags
   * @param doc the given XML node
   * @return a [[Seq]] of [[NarrativeDescriptor]]
   */
  private def parseNarratives(doc: Node) = {
    val tagName = "narrative"
    (doc \ tagName) map { node =>
      val id = node.getAttr(tagName, "id")
      val className = node.getAttr(tagName, "class")
      NarrativeDescriptor(id, className, parseProperties(node))
    }
  }

  /**
   * Parses the <code>trigger</code> tags
   * @param doc the given XML node
   * @return a [[Seq]] of [[Trigger]]
   */
  private def parseTriggers(doc: Node,
                            narrativeSeq: Seq[NarrativeDescriptor],
                            scheduleSeq: Seq[Scheduling],
                            resourceSeq: Seq[UserResource]) = {
    val narratives = Map(narrativeSeq.map(n => (n.id, n)): _*)
    val schedules = Map(scheduleSeq.map(s => (s.id, s)): _*)
    val resources = Map(resourceSeq.map(r => (r.id, r.resource)): _*)

    val tagName = "trigger"
    (doc \ "trigger") map { node =>
      val scheduleId = node.getAttr(tagName, "schedule-ref")
      val narrativeId = node.getAttr(tagName, "narrative-ref")
      val resourceId = node.getAttrOpt("resource-ref")
      val enabled = node.getAttrOpt("enabled") map (_.toLowerCase == "true") getOrElse true

      val schedule = schedules.get(scheduleId).orDie(s"Schedule '$scheduleId' was not found")
      val narrative = narratives.get(narrativeId).orDie(s"Narrative '$narrativeId' was not found")
      val resource = resourceId.flatMap(resources.get) // TODO check to see if resource is populated
      Trigger(narrative, schedule, enabled, resource)
    }
  }

  case class UserResource(id: String, resource: Resource)

}
