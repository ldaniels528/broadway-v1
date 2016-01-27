package com.github.ldaniels528.broadway.core

import java.io.File

import com.github.ldaniels528.broadway.core.StoryConfigParser._
import com.github.ldaniels528.broadway.core.io.archival.Archive
import com.github.ldaniels528.broadway.core.io.device._
import com.github.ldaniels528.broadway.core.io.device.kafka.{KafkaOutputSource, ZkProxy}
import com.github.ldaniels528.broadway.core.io.device.nosql.MongoDbOutputSource
import com.github.ldaniels528.broadway.core.io.device.text.{TextFileInputSource, TextFileOutputSource}
import com.github.ldaniels528.broadway.core.io.flow.{BasicFlow, Flow}
import com.github.ldaniels528.broadway.core.io.layout._
import com.github.ldaniels528.broadway.core.io.layout.json.{AvroLayout, MongoDbLayout}
import com.github.ldaniels528.broadway.core.io.layout.text.TextLayout
import com.github.ldaniels528.broadway.core.io.layout.text.fields._
import com.github.ldaniels528.broadway.core.io.trigger.{FileFeed, FileTrigger, StartupTrigger}
import com.ldaniels528.commons.helpers.OptionHelper._

import scala.language.postfixOps
import scala.xml.{Node, XML}

/**
  * Story Configuration Parser
  */
class StoryConfigParser(xml: Node) {

  def parse: Option[StoryConfig] = {
    ((xml \\ "etl-config") map { node =>
      StoryConfig(id = node \@ "id", triggers = parseTriggers(node))
    }).headOption
  }

  private def parseArchives(rootNode: Node) = {
    (rootNode \ "archives") flatMap { archiveNode =>
      archiveNode.child.filter(_.label != "#PCDATA") map { node =>
        node.label match {
          case "Archive" => parseArchives_File(node)
          case label =>
            throw new IllegalArgumentException(s"Invalid archive '$label'")
        }
      }
    }
  }

  private def parseArchives_File(rootNode: Node) = {
    Archive(id = rootNode \@ "id", base = new File(rootNode \@ "base"))
  }

  private def parseDevices(rootNode: Node, layouts: Seq[Layout]): Seq[IOSource] = {
    (rootNode \ "devices") flatMap { devicesNode =>
      devicesNode.child.filter(_.label != "#PCDATA") map { node =>
        node.label match {
          case "KafkaOutputSource" => parseDevices_KafkaOutputDevice(node, layouts)
          case "MongoOutputSource" => parseDevices_MongoOutputDevice(node, layouts)
          case "PooledOutputSource" => parseDevices_MultiOutputDevice(node, layouts)
          case "ConcurrentOutputSource" => parseDevices_RoundRobinOutputDevice(node, layouts)
          case "TextFileInputSource" => parseDevices_TextInputDevice(node, layouts)
          case "TextFileOutputSource" => parseDevices_TextOutputDevice(node, layouts)
          case label =>
            throw new IllegalArgumentException(s"Invalid device type '$label'")
        }
      }
    }
  }

  private def parseDevices_KafkaOutputDevice(deviceNode: Node, layouts: Seq[Layout]) = {
    KafkaOutputSource(
      id = deviceNode \@ "id",
      topic = deviceNode \@ "topic",
      zk = ZkProxy(connectionString = deviceNode \@ "connectionString"))
  }

  private def parseDevices_MongoOutputDevice(deviceNode: Node, layouts: Seq[Layout]) = {
    MongoDbOutputSource(
      id = deviceNode \@ "id",
      serverList = deviceNode \@ "servers",
      database = deviceNode \@ "database",
      collection = deviceNode \@ "collection",
      layout = lookupLayout(layouts, id = deviceNode \@ "layout").need[MongoDbLayout]("Incompatible layout"))
  }

  private def parseDevices_MultiOutputDevice(deviceNode: Node, layouts: Seq[Layout]) = {
    PooledOutputSource(
      id = deviceNode \@ "id",
      devices = parseDevices(deviceNode, layouts).only[OutputSource](_.id, "output device"),
      concurrency = optionalText(deviceNode \@ "concurrency").map(_.toInt) getOrElse 2)
  }

  private def parseDevices_RoundRobinOutputDevice(deviceNode: Node, layouts: Seq[Layout]) = {
    ConcurrentOutputSource(
      id = deviceNode \@ "id",
      devices = parseDevices(deviceNode, layouts).only[OutputSource](_.id, "output device"),
      concurrency = optionalText(deviceNode \@ "concurrency").map(_.toInt) getOrElse 2)
  }

  private def parseDevices_TextInputDevice(deviceNode: Node, layouts: Seq[Layout]) = {
    TextFileInputSource(id = deviceNode \@ "id", path = deviceNode \@ "path")
  }

  private def parseDevices_TextOutputDevice(deviceNode: Node, layouts: Seq[Layout]) = {
    TextFileOutputSource(id = deviceNode \@ "id", path = deviceNode \@ "path")
  }

  private def parseFields(rootNode: Node) = {
    (rootNode \ "fields") map { fieldsNode =>
      val `type` = requiredText(fieldsNode \@ "type")
      val fields = fieldsNode.child.filter(_.label != "#PCDATA") map { node =>
        node.label match {
          case "field" => parseFields_Field(node)
          case label =>
            throw new IllegalArgumentException(s"Invalid field type '$label'")
        }
      }

      // decode the field set by type
      `type` match {
        case "csv" => CSVFieldSet(fields)
        case "delimited" =>
          DelimitedFieldSet(
            fields = fields,
            delimiter = updateSpecials(requiredText(fieldsNode \@ "delimiter")),
            isQuoted = optionalText("quoted").exists(isYes))
        case "fixed-length" => FixedLengthFieldSet(fields)
        case "json" => JsonFieldSet(fields)
        case "inline" => FixedLengthFieldSet(fields)
        case unknown =>
          throw new IllegalArgumentException(s"Unrecognized fields type '$unknown'")
      }
    }
  }

  private def updateSpecials(s: String) = {
    s.replaceAllLiterally("\\n", "\n")
      .replaceAllLiterally("\\r", "\r")
      .replaceAllLiterally("\\t", "\t")
  }

  private def parseFields_Field(node: Node) = {
    Field(
      name = node.text.trim,
      length = optionalText(node \@ "length") map (_.toInt))
  }

  private def parseFlows(rootNode: Node, devices: Seq[IOSource], layouts: Seq[Layout]): Seq[Flow] = {
    (rootNode \ "flows") flatMap { flowsNode =>
      flowsNode.child.filter(_.label != "#PCDATA") map { node =>
        node.label match {
          case "BasicFlow" => parseFlows_BasicFlow(node, devices, layouts)
          case label =>
            throw new IllegalArgumentException(s"Invalid flow reference '$label'")
        }
      }
    }
  }

  private def parseFlows_BasicFlow(rootNode: Node, devices: Seq[IOSource], layouts: Seq[Layout]) = {
    BasicFlow(
      id = rootNode \@ "id",
      input = lookupInputDevice(devices, id = rootNode \@ "input"),
      output = lookupOutputDevice(devices, id = rootNode \@ "output"),
      inLayout = lookupLayout(layouts, id = rootNode \@ "input-layout"),
      outLayout = lookupLayout(layouts, id = rootNode \@ "output-layout"))
  }

  private def parseLayouts(rootNode: Node): Seq[Layout] = {
    (rootNode \ "layouts") flatMap { layoutsNode =>
      layoutsNode.child.filter(_.label != "#PCDATA") map { node =>
        node.label match {
          case "AvroLayout" => parseLayouts_Layout_Avro(node)
          case "MongoLayout" => parseLayouts_Layout_Mongo(node)
          case "TextLayout" => parseLayouts_Layout_Text(node)
          case label =>
            throw new IllegalArgumentException(s"Invalid layout type '$label'")
        }
      }
    }
  }

  private def parseLayouts_Layout_Avro(node: Node) = {
    AvroLayout(
      id = node \@ "id",
      fieldSet = parseFields(node).headOption orDie "Exactly one fields element was expected",
      schemaString = (node \ "schema").map(_.text).headOption orDie "Schema element required")
  }

  /**
    * Parses a layout division
    *
    * @param rootNode the given [[Node node]]
    * @param label    the given label (e.g. "body", "header" or "footer")
    * @return a collection of [[Division divisions]]
    */
  private def parseLayouts_Layout_Division(rootNode: Node, label: String) = {
    (rootNode \ label).onlyOne(s"Only one $label element is allowed") match {
      case Some(node) => Some(Division(parseFields(node)))
      case None => None
    }
  }

  private def parseLayouts_Layout_Mongo(node: Node) = {
    MongoDbLayout(
      id = node \@ "id",
      fieldSet = parseFields(node).headOption orDie "Exactly one fields element was expected")
  }

  private def parseLayouts_Layout_Text(node: Node) = {
    TextLayout(
      id = node \@ "id",
      header = parseLayouts_Layout_Division(node, "header"),
      body = parseLayouts_Layout_Division(node, "body") orDie "The <body> element is required",
      footer = parseLayouts_Layout_Division(node, "footer"))
  }

  private def parseTriggers(rootNode: Node) = {
    val layouts = parseLayouts(rootNode)
    val devices = parseDevices(rootNode, layouts)
    val archives = parseArchives(rootNode)
    (rootNode \ "triggers") flatMap { triggerNode =>
      triggerNode.child.filter(_.label != "#PCDATA") map { node =>
        node.label match {
          case "FileTrigger" => parseTriggers_File(node, archives, devices, layouts)
          case "StartUpTrigger" => parseTriggers_Startup(node, parseFlows(node, devices, layouts))
          case label =>
            throw new IllegalArgumentException(s"Invalid trigger type '$label'")
        }
      }
    }
  }

  private def parseTriggers_File(rootNode: Node, archives: Seq[Archive], devices: Seq[IOSource], layouts: Seq[Layout]) = {
    FileTrigger(
      id = rootNode \@ "id",
      path = rootNode \@ "path",
      feeds = parseTriggers_File_Feed(rootNode, devices, layouts),
      archive = optionalText(rootNode \@ "archive").map(id => lookupArchive(archives, id))
    )
  }

  private def parseTriggers_File_Feed(rootNode: Node, devices: Seq[IOSource], layouts: Seq[Layout]) = {
    (rootNode \ "feed") map { feedNode =>
      FileFeed(name = feedNode \@ "name", matchType = feedNode \@ "match", flows = parseFlows(feedNode, devices, layouts))
    }
  }

  private def parseTriggers_Startup(rootNode: Node, flows: Seq[Flow]) = {
    StartupTrigger(id = rootNode \@ "id", flows)
  }

  private def lookupArchive(archives: Seq[Archive], id: String) = {
    archives.find(_.id == id) orDie s"Archive '$id' not found"
  }

  private def lookupInputDevice(devices: Seq[IOSource], id: String) = {
    devices.find(_.id == id) match {
      case Some(device: InputSource) => device
      case Some(device) =>
        throw new IllegalStateException(s"IOSource '${device.id}' is not an input device")
      case None =>
        throw new IllegalStateException(s"IOSource '$id' was not found")
    }
  }

  private def lookupLayout(layouts: Seq[Layout], id: String) = {
    layouts.find(_.id == id) orDie s"Layout '$id' was not found"
  }

  private def lookupOutputDevice(devices: Seq[IOSource], id: String) = {
    devices.find(_.id == id) match {
      case Some(device: OutputSource) => device
      case Some(device) =>
        throw new IllegalStateException(s"IOSource '${device.id}' is not an output device")
      case None =>
        throw new IllegalStateException(s"IOSource '$id' was not found")
    }
  }

  private def isYes(s: String) = s == "y" || s == "yes" || s == "t" || s == "true"

  private def optionalText(s: String) = if (s.isEmpty) None else Some(s)

  private def requiredText(s: String) = optionalText(s).orDie(s"Required property '$s' is missing")

}

/**
  * Etl Config Parser
  */
object StoryConfigParser {

  def apply(file: File) = new StoryConfigParser(XML.loadFile(file))

  /**
    * Type Conversion
    *
    * @param entity the given entity
    * @tparam T the entity type
    */
  implicit class TypeConversion[T](val entity: T) extends AnyVal {

    def need[A](message: String): A = entity match {
      case converted: A => converted
      case _ =>
        throw new IllegalArgumentException(message)
    }

  }

  implicit class SequenceEnrichment[T](val values: Seq[T]) extends AnyVal {

    def only[A](name: T => String, typeName: String): Seq[A] = values map {
      case value: A => value
      case value =>
        throw new IllegalArgumentException(s"${name(value)} is not a $typeName")
    }

    def pull[A]: Seq[A] = values flatMap {
      case value: A => Some(value)
      case _ => None
    }

    def onlyOne(message: String): Option[T] = {
      values.length match {
        case 0 => None
        case 1 => values.headOption
        case _ =>
          throw new IllegalArgumentException(message)
      }
    }

  }

}