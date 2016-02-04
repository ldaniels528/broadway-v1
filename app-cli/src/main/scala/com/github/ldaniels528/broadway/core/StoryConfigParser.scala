package com.github.ldaniels528.broadway.core

import java.io.{File, FileInputStream}

import com.github.ldaniels528.broadway.core.StoryConfigParser._
import com.github.ldaniels528.broadway.core.io.archive.Archive
import com.github.ldaniels528.broadway.core.io.archive.impl.FileArchive
import com.github.ldaniels528.broadway.core.io.device._
import com.github.ldaniels528.broadway.core.io.device.impl.SQLOutputSource.SQLConnectionInfo
import com.github.ldaniels528.broadway.core.io.device.impl._
import com.github.ldaniels528.broadway.core.io.filters.Filter
import com.github.ldaniels528.broadway.core.io.filters.impl.{DateFilter, TrimFilter}
import com.github.ldaniels528.broadway.core.io.flow._
import com.github.ldaniels528.broadway.core.io.flow.impl.{CompositeInputFlow, CompositeOutputFlow, SimpleFlow}
import com.github.ldaniels528.broadway.core.io.layout._
import com.github.ldaniels528.broadway.core.io.layout.impl.MultiPartLayout
import com.github.ldaniels528.broadway.core.io.layout.impl.MultiPartLayout.Section
import com.github.ldaniels528.broadway.core.io.record._
import com.github.ldaniels528.broadway.core.io.record.impl._
import com.github.ldaniels528.broadway.core.io.trigger.impl.{FileFeed, FileFeedDirectory, FileTrigger, StartupTrigger}
import com.github.ldaniels528.broadway.core.support.kafka.ZkProxy
import com.ldaniels528.commons.helpers.OptionHelper._
import com.mongodb.casbah.Imports._

import scala.collection.JavaConversions._
import scala.language.postfixOps
import scala.reflect.ClassTag
import scala.xml.{Node, XML}

/**
  * Story Configuration Parser
  * @author lawrence.daniels@gmail.com
  */
class StoryConfigParser() {

  def parse(xml: Node): Seq[StoryConfig] = {
    (xml \\ "story") map { node =>
      // first create the base configuration; processing all import statements
      val baseConfig: StoryConfig = parseImports(node)
        .foldLeft(StoryConfig(id = "baseConfig", filters = getbuiltInFilters)) { (accumulator, config) =>
          accumulator.copy(
            archives = (config.archives ++ accumulator.archives).dedup(_.id),
            devices = (config.devices ++ accumulator.devices).dedup(_.id),
            filters = (config.filters ++ accumulator.filters).dedup(_._1),
            layouts = (config.layouts ++ accumulator.layouts).dedup(_.id),
            properties = (config.properties ++ accumulator.properties).dedup(_._1),
            triggers = (config.triggers ++ accumulator.triggers).dedup(_.id)
          )
        }

      // next, load the objects for the current config appending the base config objects
      val archives = (parseArchives(node) ++ baseConfig.archives).dedup(_.id)
      val filters = (parseFilters(node) ++ baseConfig.filters).dedup(_._1)
      val layouts = (parseLayouts(node) ++ baseConfig.layouts).dedup(_.id)
      val devices = (parseDataSources(node, layouts) ++ baseConfig.devices).dedup(_.id)
      val properties = (parseProperties(node) ++ baseConfig.properties).dedup(_._1)
      val triggers = (parseTriggers(baseConfig, node, archives, devices, layouts) ++ baseConfig.triggers).dedup(_.id)

      // finally, return the composite config
      StoryConfig(
        id = node \\@ "id",
        archives = archives,
        devices = devices,
        filters = filters,
        layouts = layouts,
        properties = properties,
        triggers = triggers)
    }
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

  private def parseFilters(rootNode: Node): Seq[(String, Filter)] = {
    // TODO allow user-defined filters
    Nil
  }

  private def parseArchives_File(rootNode: Node): Archive = {
    FileArchive(id = rootNode \\@ "id", basePath = rootNode \\@ "base")
  }

  private def parseDataSources(rootNode: Node, layouts: Seq[Layout]): Seq[DataSource] = {
    (rootNode \ "data-sources") flatMap { devicesNode =>
      devicesNode.child.filter(_.label != "#PCDATA") map { node =>
        node.label match {
          case "CompositeOutputSource" => parseDataSources_CompositeOutputSource(node, layouts)
          case "ConcurrentOutputSource" => parseDataSources_ConcurrentOutput(node, layouts)
          case "KafkaOutputSource" => parseDataSources_KafkaOutput(node, layouts)
          case "MongoOutputSource" => parseDataSources_MongoOutput(node, layouts)
          case "SQLOutputSource" => parseDataSources_SQLOutput(node, layouts)
          case "TextFileInputSource" => parseDataSources_TextFileInput(node, layouts)
          case "TextFileOutputSource" => parseDataSources_TextFileOutput(node, layouts)
          case label =>
            throw new IllegalArgumentException(s"Invalid data source type '$label'")
        }
      }
    }
  }

  private def parseDataSources_CompositeOutputSource(node: Node, layouts: Seq[Layout]) = {
    CompositeOutputSource(
      id = node \\@ "id",
      devices = parseDataSources(node, layouts).requireType[OutputSource](_.id, "output source"))
  }

  private def parseDataSources_ConcurrentOutput(node: Node, layouts: Seq[Layout]) = {
    ConcurrentOutputSource(
      id = node \\@ "id",
      concurrency = (node \?@ "concurrency").map(_.toInt) getOrElse 2,
      devices = parseDataSources(node, layouts).requireType[OutputSource](_.id, "output source"))
  }

  private def parseDataSources_KafkaOutput(node: Node, layouts: Seq[Layout]) = {
    KafkaOutputSource(
      id = node \\@ "id",
      topic = node \\@ "topic",
      zk = ZkProxy(connectionString = node \\@ "connectionString"),
      layout = lookupLayout(layouts, id = node \\@ "layout"))
  }

  private def parseDataSources_MongoOutput(node: Node, layouts: Seq[Layout]) = {
    MongoDbOutputSource(
      id = node \\@ "id",
      serverList = node \\@ "servers",
      database = node \\@ "database",
      collection = node \\@ "collection",
      writeConcern = parseDataSources_Mongo_WriteConcern(node \?@ "write-concern"),
      layout = lookupLayout(layouts, id = node \\@ "layout"))
  }

  private def parseDataSources_Mongo_WriteConcern(concern_? : Option[String]) = {
    concern_? match {
      case Some(concern) => WriteConcern.valueOf(concern) orDie s"Invalid write concern '$concern'"
      case None => WriteConcern.JournalSafe
    }
  }

  private def parseDataSources_SQLOutput(node: Node, layouts: Seq[Layout]) = {
    SQLOutputSource(
      id = node \\@ "id",
      connectionInfo = SQLConnectionInfo(
        driver = node \\@ "driver",
        url = node \\@ "url",
        user = node \\@ "user",
        password = node \\@ "password"),
      layout = lookupLayout(layouts, id = node \\@ "layout"))
  }

  private def parseDataSources_TextFileInput(node: Node, layouts: Seq[Layout]) = {
    TextFileInputSource(id = node \\@ "id", path = node \\@ "path", layout = lookupLayout(layouts, node \\@ "layout"))
  }

  private def parseDataSources_TextFileOutput(node: Node, layouts: Seq[Layout]) = {
    TextFileOutputSource(id = node \\@ "id", path = node \\@ "path", layout = lookupLayout(layouts, node \\@ "layout"))
  }

  private def parseFlows(rootNode: Node, devices: Seq[DataSource], layouts: Seq[Layout]): Seq[Flow] = {
    rootNode.child.filter(_.label != "#PCDATA") map { node =>
      node.label match {
        case "CompositeInputFlow" => parseFlows_CompositeInput(node, devices)
        case "CompositeOutputFlow" => parseFlows_CompositeOutput(node, devices)
        case "SimpleFlow" => parseFlows_Simple(node, devices)
        case label =>
          throw new IllegalArgumentException(s"Invalid flow reference '$label'")
      }
    }
  }

  private def parseFlows_CompositeInput(node: Node, devices: Seq[DataSource]) = {
    CompositeInputFlow(
      id = node \\@ "id",
      output = lookupOutputDataSource(devices, id = node \\@ "output-source"),
      inputs = (node \ "include") map { includeNode =>
        lookupInputDataSource(devices, id = includeNode \\@ "input-source")
      })
  }

  private def parseFlows_CompositeOutput(node: Node, devices: Seq[DataSource]) = {
    CompositeOutputFlow(
      id = node \\@ "id",
      input = lookupInputDataSource(devices, id = node \\@ "input-source"),
      outputs = (node \ "include") map { includeNode =>
        lookupOutputDataSource(devices, id = includeNode \\@ "output-source")
      })
  }

  private def parseFlows_Simple(node: Node, devices: Seq[DataSource]) = {
    SimpleFlow(
      id = node \\@ "id",
      input = lookupInputDataSource(devices, id = node \\@ "input-source"),
      output = lookupOutputDataSource(devices, id = node \\@ "output-source"))
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

  private def parseImports(rootNode: Node) = {
    (rootNode \\ "import") flatMap { importNode =>
      val file = new File(importNode \\@ "path")
      StoryConfigParser.parse(file)
    }
  }

  /**
    * Parses a layout section
    * @param rootNode the given [[Node node]]
    * @param label    the given label (e.g. "body", "header" or "footer")
    * @return a collection of [[Section sections]]
    */
  private def parseLayouts_Layout_Section(rootNode: Node, label: String) = {
    rootNode \- label match {
      case Some(node) => Some(Section(parseRecord(node)))
      case None => None
    }
  }

  private def parseLayouts_Layout_MultiPart(node: Node) = {
    MultiPartLayout(
      id = node \\@ "id",
      header = parseLayouts_Layout_Section(node, "header"),
      body = parseLayouts_Layout_Section(node, "body") orDie "The <body> element is required",
      footer = parseLayouts_Layout_Section(node, "footer"))
  }

  private def parseProperties(rootNode: Node) = {
    (rootNode \\ "properties") flatMap { propsNode =>
      val file = propsNode \\@ "file"
      val props = new java.util.Properties()
      props.load(new FileInputStream(file))
      props.toMap.toSeq
    }
  }

  private def parseRecord(rootNode: Node) = {
    (rootNode \ "record") map { fieldsNode =>
      val id = fieldsNode \\@ "id"
      val `type` = fieldsNode \\@ "type"
      val fields = fieldsNode.child.filter(_.label != "#PCDATA") map { node =>
        node.label match {
          case "field" => parseRecord_Field(id, node)
          case label =>
            throw new IllegalArgumentException(s"Invalid field type '$label'")
        }
      }

      // decode the field set by type
      `type` match {
        case "avro" => AvroRecord(id = id, name = fieldsNode \\@ "name", namespace = fieldsNode \\@ "namespace", fields = fields)
        case "csv" => CsvRecord(id = id, fields = fields)
        case "delimited" => DelimitedRecord(id = id, fields = fields, delimiter = fieldsNode \\+@ "delimiter")
        case "fixed-length" => FixedLengthRecord(id = id, fields = fields)
        case "generic" => GenericRecord(id = id, fields = fields)
        case "json" => JsonRecord(id = id, fields = fields)
        case "sql" => SQLRecord(id = id, table = fieldsNode \?@ "table" getOrElse id, fields = fields)
        case unknown =>
          throw new IllegalArgumentException(s"Unrecognized fields type '$unknown'")
      }
    }
  }

  private def parseRecord_Field(recordId: String, node: Node) = {
    val name = node \\@ "name"
    Field(
      name = name,
      path = s"$recordId.$name",
      `type` = (node \?@ "type").map(_.toUpperCase) map DataTypes.withName getOrElse DataTypes.STRING,
      defaultValue = node \?@ "value" ?? node.text_?,
      length = (node \?@ "length") map (_.toInt))
  }

  private def parseTriggers(baseConfig: StoryConfig, rootNode: Node, archives: Seq[Archive], devices: Seq[DataSource], layouts: Seq[Layout]) = {
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

  private def parseTriggers_FileTrigger(rootNode: Node, archives: Seq[Archive], devices: Seq[DataSource], layouts: Seq[Layout]) = {
    FileTrigger(
      id = rootNode \\@ "id",
      directories = parseTriggers_FileTrigger_Directories(rootNode, archives, devices, layouts))
  }

  private def parseTriggers_FileTrigger_Directories(rootNode: Node, archives: Seq[Archive], devices: Seq[DataSource], layouts: Seq[Layout]) = {
    (rootNode \ "directory") map { directoryNode =>
      FileFeedDirectory(
        path = directoryNode \\@ "path",
        feeds = parseTriggers_FileTrigger_Feeds(directoryNode, archives, devices, layouts),
        archive = (directoryNode \?@ "archive").map(lookupArchive(archives, _)))
    }
  }

  private def parseTriggers_FileTrigger_Feeds(rootNode: Node, archives: Seq[Archive], devices: Seq[DataSource], layouts: Seq[Layout]) = {
    (rootNode \ "feed") map { feedNode =>
      val flows = parseFlows(feedNode, devices, layouts)
      val archive = (rootNode \?@ "archive").map(id => lookupArchive(archives, id))

      (feedNode \?@ "endsWith").map(suffix => FileFeed.endsWith(suffix, flows, archive)) ??
        (feedNode \?@ "name").map(name => FileFeed.exact(name, flows, archive)) ??
        (feedNode \?@ "pattern").map(pattern => FileFeed.regex(pattern, flows, archive)) ??
        (feedNode \?@ "startsWith").map(prefix => FileFeed.startsWith(prefix, flows, archive)) orDie s"Invalid feed definition - $feedNode"
    }
  }

  private def parseTriggers_Startup(rootNode: Node, flows: Seq[Flow]) = {
    StartupTrigger(id = rootNode \\@ "id", flows)
  }

  private def getbuiltInFilters = Seq(
    "date" -> DateFilter(),
    "trim" -> TrimFilter()
  )

  private def lookupArchive(archives: Seq[Archive], id: String) = {
    archives.find(_.id == id) orDie s"Archive '$id' not found"
  }

  private def lookupDataSource[A <: DataSource](devices: Seq[DataSource], id: String, friendlyTypeName: String)(implicit tag: ClassTag[A]) = {
    devices.find(_.id == id) match {
      case Some(device: A) => device
      case Some(device) =>
        throw new IllegalStateException(s"Source '${device.id}' is not an $friendlyTypeName")
      case None =>
        throw new IllegalStateException(s"Source '$id' was not found")
    }
  }

  private def lookupInputDataSource(devices: Seq[DataSource], id: String) = {
    lookupDataSource[InputSource](devices, id, "input source")
  }

  private def lookupLayout(layouts: Seq[Layout], id: String) = {
    layouts.find(_.id == id) orDie s"Layout '$id' was not found"
  }

  private def lookupOutputDataSource(devices: Seq[DataSource], id: String) = {
    lookupDataSource[OutputSource](devices, id, "output source")
  }

}

/**
  * Story Config Parser
  * @author lawrence.daniels@gmail.com
  */
object StoryConfigParser {

  def parse(file: File) = {
    new StoryConfigParser().parse(XML.loadFile(file))
  }

  /**
    * Node Enrichment
    * @param node the given [[Node XML node]]
    */
  implicit class NodeEnrichment(val node: Node) extends AnyVal {

    /**
      * Ensures only a single element exists, and if so, returns it
      * @param name the name of the element
      * @return an option of the element [[Node node]]
      */
    @throws[IllegalArgumentException]
    def \-(name: String) = {
      val nodeSeq = node \ name
      if (nodeSeq.theSeq.length > 1)
        throw new IllegalArgumentException(s"Only a single $name element was expected")
      nodeSeq.headOption
    }

    /**
      * Returns the required attribute, or throws an [[IllegalArgumentException exception]]
      * @param name the name of the attribute
      * @return the attribute's value
      */
    @throws[IllegalArgumentException]
    def \\@(name: String) = {
      val s = node \@ name
      if (s.trim.isEmpty) throw new IllegalArgumentException(s"Attribute $name is required in element ${node.label}")
      s
    }

    def \\+@(name: String) = {
      (node \\@ name).replaceAllLiterally("\\n", "\n").replaceAllLiterally("\\r", "\r").replaceAllLiterally("\\t", "\t")
    }

    def \?@(name: String) = {
      val s = node \@ name
      if (s.trim.isEmpty) None else Option(s)
    }

    def text_? = {
      val s = node.text
      if (s.trim.isEmpty) None else Some(s)
    }

  }

  /**
    * Sequence Enrichment
    * @param values the given sequence to enrich
    * @tparam T the sequence's template type
    */
  implicit class SequenceEnrichment[T](val values: Seq[T]) extends AnyVal {

    def dedup(id: T => String) = Map(values.map(v => id(v) -> v): _*).values.toSeq

    def requireType[A <: T](name: T => String, typeName: String)(implicit tagA: ClassTag[A], tagT: ClassTag[T]): Seq[A] = values map {
      case value: A => value
      case value =>
        throw new IllegalArgumentException(s"${name(value)} is not a $typeName")
    }

  }

}