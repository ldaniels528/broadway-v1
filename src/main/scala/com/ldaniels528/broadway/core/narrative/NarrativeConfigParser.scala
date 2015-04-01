package com.ldaniels528.broadway.core.narrative

import java.util.Properties

import com.ldaniels528.broadway.core.location.{FileLocation, HttpLocation, Location}
import com.ldaniels528.broadway.core.resources._
import com.ldaniels528.broadway.core.schedules.Scheduling
import com.ldaniels528.broadway.core.triggers.Trigger
import com.ldaniels528.broadway.core.util.XMLHelper._
import com.ldaniels528.trifecta.util.OptionHelper._
import com.ldaniels528.trifecta.util.PropertiesHelper._

import scala.util.{Failure, Success, Try}
import scala.xml.{Node, XML}

/**
 * Topology Configuration Parser Singleton
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
object NarrativeConfigParser {

  /**
   * Parses the given resource and returns an option of a narrative configuration
   * @param resource the given [[ReadableResource]]
   * @return an option of a [[NarrativeConfig]]
   */
  def parse(resource: ReadableResource): Option[NarrativeConfig] = {
    resource.getInputStream map { in =>
      val doc = XML.load(in)
      val narratives = parseNarratives(doc)
      val schedules = parseSchedules(doc)
      val resources = parseResources(doc)
      val triggers = parseTriggers(doc, narratives, schedules, resources)

      new NarrativeConfig(
        locations = parseLocations(narratives, doc),
        propertySets = parsePropertiesRef(doc),
        schedules,
        narratives,
        triggers)
    }
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

  private def parseResources(doc: Node) = {
    val tagName = "resource"
    (doc \ tagName) map { node =>
      val id = node.getAttr(tagName, "id")
      val className = node.getAttr(tagName, "class")

      instantiate(id, className) match {
        case Success(resource: Resource) => UserResource(id, resource)
        case Success(unknown) =>
          throw new IllegalStateException(s"Resource '$id' (class $className}) does not implement ${classOf[Resource].getName}")
        case Failure(e) =>
          throw new IllegalStateException(s"Failed to load resource '$id' (class $className})")
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

  private def instantiate(id: String, className: String) = Try {
    val `class` = Class.forName(className)
    val constructor = `class`.getConstructor(classOf[String])
    constructor.newInstance(id)
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
      NarrativeDescriptor(id, className, getTopologyPropertiesRef(node))
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

      val schedule = schedules.get(scheduleId).orDie(s"Schedule '$scheduleId' was not found")
      val narrative = narratives.get(narrativeId).orDie(s"Narrative '$narrativeId' was not found")
      val resource = resourceId.flatMap(resources.get) // TODO check to see if resource is populated
      Trigger(narrative, schedule, resource)
    }
  }

  private def getTopologyPropertiesRef(node: Node): NarrativeRuntime => Properties = {
    { rt =>
      node.getAttrOpt("properties-ref") match {
        case Some(refId) => rt.getPropertiesByID(refId).orDie(s"A properties set for '$refId' could not be found")
        case None => Map(parseProperty(node): _*).toProps
      }
    }
  }

  case class UserResource(id: String, resource: Resource)

}
