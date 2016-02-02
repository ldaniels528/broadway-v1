package com.github.ldaniels528.broadway.core

import java.io.File

import com.github.ldaniels528.broadway.core.StoryConfigParser._
import com.github.ldaniels528.broadway.core.io.archive.FileArchive
import com.github.ldaniels528.broadway.core.io.device._
import com.github.ldaniels528.broadway.core.io.device.kafka.ZkProxy
import com.github.ldaniels528.broadway.core.io.flow._
import com.github.ldaniels528.broadway.core.io.layout.MultiPartLayout.Section
import com.github.ldaniels528.broadway.core.io.layout._
import com.github.ldaniels528.broadway.core.io.trigger.{FileFeed, FileFeedDirectory, FileTrigger, StartupTrigger}
import com.ldaniels528.commons.helpers.OptionHelper._
import com.mongodb.casbah.Imports._

import scala.language.postfixOps
import scala.reflect.ClassTag
import scala.xml.{Node, XML}

/**
  * Story Configuration Parser
  */
class StoryConfigParser(xml: Node) {

  def parse: Option[StoryConfig] = {
    (xml \\ "story") map { node =>
      StoryConfig(id = node \@ "id", triggers = parseTriggers(node))
    } headOption
  }

  private def parseArchives(rootNode: Node) = {
    (rootNode \ "archives") flatMap { archiveNode =>
      archiveNode.child.filter(_.label != "#PCDATA") map { node =>
        node.label match {
          case "FileArchive" => parseArchives_File(node)
          case label =>
            throw new IllegalArgumentException(s"Invalid archive '$label'")
        }
      }
    }
  }

  private def parseArchives_File(rootNode: Node) = {
    FileArchive(id = rootNode \@ "id", base = new File(rootNode \@ "base"))
  }

  private def parseDataSources(rootNode: Node, layouts: Seq[Layout]): Seq[DataSource] = {
    (rootNode \ "data-sources") flatMap { devicesNode =>
      devicesNode.child.filter(_.label != "#PCDATA") map { node =>
        node.label match {
          case "KafkaOutputSource" => parseDataSources_KafkaOutputDataSource(node, layouts)
          case "MongoOutputSource" => parseDataSources_MongoOutputDataSource(node, layouts)
          case "ConcurrentOutputSource" => parseDataSources_ConcurrentOutputSource(node, layouts)
          case "TextFileInputSource" => parseDataSources_TextInputDataSource(node, layouts)
          case "TextFileOutputSource" => parseDataSources_TextOutputDataSource(node, layouts)
          case label =>
            throw new IllegalArgumentException(s"Invalid data source type '$label'")
        }
      }
    }
  }

  private def parseDataSources_KafkaOutputDataSource(node: Node, layouts: Seq[Layout]) = {
    KafkaOutputSource(
      id = node \@ "id",
      topic = node \@ "topic",
      zk = ZkProxy(connectionString = node \@ "connectionString"),
      layout = lookupLayout(layouts, id = node \@ "layout"))
  }

  private def parseDataSources_MongoOutputDataSource(node: Node, layouts: Seq[Layout]) = {
    MongoDbOutputSource(
      id = node \@ "id",
      serverList = node \@ "servers",
      database = node \@ "database",
      collection = node \@ "collection",
      writeConcern = (node \@ "write-concern").optional.map(getWriteConcern).getOrElse(WriteConcern.JournalSafe),
      layout = lookupLayout(layouts, id = node \@ "layout"))
  }

  private def getWriteConcern(concern: String) = {
    WriteConcern.valueOf(concern) orDie s"Invalid write concern '$concern'"
  }

  private def parseDataSources_ConcurrentOutputSource(node: Node, layouts: Seq[Layout]) = {
    ConcurrentOutputSource(
      id = node \@ "id",
      concurrency = (node \@ "concurrency").optional.map(_.toInt) getOrElse 2,
      devices = parseDataSources(node, layouts).only[OutputSource](_.id, "output device"))
  }

  private def parseDataSources_TextInputDataSource(node: Node, layouts: Seq[Layout]) = {
    TextFileInputSource(id = node \@ "id", path = node \@ "path", layout = lookupLayout(layouts, node \@ "layout"))
  }

  private def parseDataSources_TextOutputDataSource(node: Node, layouts: Seq[Layout]) = {
    TextFileOutputSource(id = node \@ "id", path = node \@ "path", layout = lookupLayout(layouts, node \@ "layout"))
  }

  private def parseFlows(rootNode: Node, devices: Seq[DataSource], layouts: Seq[Layout]): Seq[Flow] = {
    rootNode.child.filter(_.label != "#PCDATA") map { node =>
      node.label match {
        case "CompositeInputFlow" => parseFlows_CompositeInputFlow(node, devices)
        case "SimpleFlow" => parseFlows_SimpleFlow(node, devices)
        case label =>
          throw new IllegalArgumentException(s"Invalid flow reference '$label'")
      }
    }
  }

  private def parseFlows_CompositeInputFlow(node: Node, devices: Seq[DataSource]) = {
    CompositeInputFlow(
      id = node \@ "id",
      output = lookupOutputDataSource(devices, id = node \@ "output-source"),
      inputs = (node \ "include") map { includeNode =>
        lookupInputDataSource(devices, id = includeNode \@ "input-source")
      })
  }

  private def parseFlows_SimpleFlow(node: Node, devices: Seq[DataSource]) = {
    SimpleFlow(
      id = node \@ "id",
      input = lookupInputDataSource(devices, id = node \@ "input-source"),
      output = lookupOutputDataSource(devices, id = node \@ "output-source"))
  }

  private def parseLayouts(rootNode: Node): Seq[Layout] = {
    (rootNode \ "layouts") flatMap { layoutsNode =>
      layoutsNode.child.filter(_.label != "#PCDATA") map { node =>
        node.label match {
          case "MultiPartLayout" => parseLayouts_Layout_MultiPart(node)
          case label =>
            throw new IllegalArgumentException(s"Invalid layout type '$label'")
        }
      }
    }
  }

  /**
    * Parses a layout section
    *
    * @param rootNode the given [[Node node]]
    * @param label    the given label (e.g. "body", "header" or "footer")
    * @return a collection of [[Section sections]]
    */
  private def parseLayouts_Layout_Section(rootNode: Node, label: String) = {
    (rootNode \ label).onlyOne(s"Only one $label element is allowed") match {
      case Some(node) => Some(Section(parseRecord(node)))
      case None => None
    }
  }

  private def parseLayouts_Layout_MultiPart(node: Node) = {
    MultiPartLayout(
      id = node \@ "id",
      header = parseLayouts_Layout_Section(node, "header"),
      body = parseLayouts_Layout_Section(node, "body") orDie "The <body> element is required",
      footer = parseLayouts_Layout_Section(node, "footer"))
  }

  private def parseRecord(rootNode: Node) = {
    (rootNode \ "record") map { fieldsNode =>
      val id = (fieldsNode \@ "id").required
      val `type` = (fieldsNode \@ "type").required
      val fields = fieldsNode.child.filter(_.label != "#PCDATA") map { node =>
        node.label match {
          case "field" => parseRecord_Field(id, node)
          case label =>
            throw new IllegalArgumentException(s"Invalid field type '$label'")
        }
      }

      // decode the field set by type
      `type` match {
        case "avro" => AvroRecord(id = id, name = (fieldsNode \@ "name").required, namespace = (fieldsNode \@ "namespace").required, fields = fields)
        case "csv" => CsvRecord(id = id, fields = fields)
        case "delimited" => DelimitedRecord(id = id, fields = fields, delimiter = (fieldsNode \@ "delimiter").required.specialChars)
        case "fixed-length" => FixedLengthRecord(id = id, fields = fields)
        case "json" => JsonRecord(id = id, fields = fields)
        case unknown =>
          throw new IllegalArgumentException(s"Unrecognized fields type '$unknown'")
      }
    }
  }

  private def parseRecord_Field(recordId: String, node: Node) = {
    val name = (node \@ "name").required
    Field(
      name = name,
      path = s"$recordId.$name",
      `type` = (node \@ "type").optional.map(s => DataTypes.withName(s.toUpperCase)) getOrElse DataTypes.STRING,
      defaultValue = (node \@ "value").optional,
      length = (node \@ "length").optional map (_.toInt))
  }

  private def parseTriggers(rootNode: Node) = {
    val layouts = parseLayouts(rootNode)
    val devices = parseDataSources(rootNode, layouts)
    val archives = parseArchives(rootNode)
    (rootNode \ "triggers") flatMap { triggerNode =>
      triggerNode.child.filter(_.label != "#PCDATA") map { node =>
        node.label match {
          case "FileTrigger" => parseTriggers_FileTrigger(node, archives, devices, layouts)
          case "StartUpTrigger" => parseTriggers_Startup(node, parseFlows(node, devices, layouts))
          case label =>
            throw new IllegalArgumentException(s"Invalid trigger type '$label'")
        }
      }
    }
  }

  private def parseTriggers_FileTrigger(rootNode: Node, archives: Seq[FileArchive], devices: Seq[DataSource], layouts: Seq[Layout]) = {
    FileTrigger(
      id = rootNode \@ "id",
      directories = parseTriggers_FileTrigger_Directories(rootNode, archives, devices, layouts))
  }

  private def parseTriggers_FileTrigger_Directories(rootNode: Node, archives: Seq[FileArchive], devices: Seq[DataSource], layouts: Seq[Layout]) = {
    (rootNode \ "directory") map { directoryNode =>
      FileFeedDirectory(
        path = (directoryNode \@ "path").required,
        feeds = parseTriggers_FileTrigger_Feeds(directoryNode, archives, devices, layouts),
        archive = (directoryNode \@ "archive").optional.map(lookupArchive(archives, _)))
    }
  }

  private def parseTriggers_FileTrigger_Feeds(rootNode: Node, archives: Seq[FileArchive], devices: Seq[DataSource], layouts: Seq[Layout]) = {
    (rootNode \ "feed") map { feedNode =>
      val flows = parseFlows(feedNode, devices, layouts)
      val archive = (rootNode \@ "archive").optional.map(id => lookupArchive(archives, id))

      (feedNode \@ "endsWith").optional.map(suffix => FileFeed.endsWith(suffix, flows, archive)) ??
        (feedNode \@ "name").optional.map(name => FileFeed.exact(name, flows, archive)) ??
        (feedNode \@ "pattern").optional.map(pattern => FileFeed.regex(pattern, flows, archive)) ??
        (feedNode \@ "startsWith").optional.map(prefix => FileFeed.startsWith(prefix, flows, archive)) orDie s"Invalid feed definition - $feedNode"
    }
  }

  private def parseTriggers_Startup(rootNode: Node, flows: Seq[Flow]) = {
    StartupTrigger(id = rootNode \@ "id", flows)
  }

  private def lookupArchive(archives: Seq[FileArchive], id: String) = {
    archives.find(_.id == id) orDie s"Archive '$id' not found"
  }

  private def lookupInputDataSource(devices: Seq[DataSource], id: String) = {
    devices.find(_.id == id) match {
      case Some(device: InputSource) => device
      case Some(device) =>
        throw new IllegalStateException(s"Source '${device.id}' is not an input device")
      case None =>
        throw new IllegalStateException(s"Source '$id' was not found")
    }
  }

  private def lookupLayout(layouts: Seq[Layout], id: String) = {
    layouts.find(_.id == id) orDie s"Layout '$id' was not found"
  }

  private def lookupOutputDataSource(devices: Seq[DataSource], id: String) = {
    devices.find(_.id == id) match {
      case Some(device: OutputSource) => device
      case Some(device) =>
        throw new IllegalStateException(s"Source '${device.id}' is not an output device")
      case None =>
        throw new IllegalStateException(s"Source '$id' was not found")
    }
  }

}

/**
  * Etl Config Parser
  */
object StoryConfigParser {

  def apply(file: File) = new StoryConfigParser(XML.loadFile(file))

  /**
    * String Enrichment
    *
    * @param s the string to enrich
    */
  implicit class StringEnrichment(val s: String) extends AnyVal {

    def isTrue = s == "true" || s == "yes"

    def optional = if (s.isEmpty) None else Some(s)

    def required = s.optional.orDie(s"Required property '$s' is missing")

    def specialChars = {
      s.replaceAllLiterally("\\n", "\n").replaceAllLiterally("\\r", "\r").replaceAllLiterally("\\t", "\t")
    }

  }

  /**
    * Type Conversion
    *
    * @param entity the given entity
    * @tparam T the entity type
    */
  implicit class TypeConversion[T](val entity: T) extends AnyVal {

    def need[A](message: String)(implicit tag: ClassTag[A]): A = entity match {
      case converted: A => converted
      case _ =>
        throw new IllegalArgumentException(message)
    }

  }

  /**
    * Sequence Enrichment
    *
    * @param values the given sequence to enrich
    * @tparam T the sequence's template type
    */
  implicit class SequenceEnrichment[T](val values: Seq[T]) extends AnyVal {

    def only[A](name: T => String, typeName: String)(implicit tag: ClassTag[A]): Seq[A] = values map {
      case value: A => value
      case value =>
        throw new IllegalArgumentException(s"${name(value)} is not a $typeName")
    }

    def pull[A](implicit tag: ClassTag[A]): Seq[A] = values flatMap {
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