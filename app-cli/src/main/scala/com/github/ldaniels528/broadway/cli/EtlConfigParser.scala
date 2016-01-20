package com.github.ldaniels528.broadway.cli

import java.io.File

import com.github.ldaniels528.broadway.cli.flow.{Flow, SimpleFlow}
import com.github.ldaniels528.broadway.cli.io.device._
import com.github.ldaniels528.broadway.cli.io.device.kafka.{KafkaOutputDevice, ZkProxy}
import com.github.ldaniels528.broadway.cli.io.device.nosql.MongoDbOutputDevice
import com.github.ldaniels528.broadway.cli.io.device.text.{TextInputDevice, TextOutputDevice}
import com.github.ldaniels528.broadway.cli.io.layout._
import com.github.ldaniels528.broadway.cli.io.layout.avro.AvroLayout
import com.github.ldaniels528.broadway.cli.io.layout.text.TextLayout
import com.github.ldaniels528.broadway.cli.io.layout.text.fields._
import com.ldaniels528.commons.helpers.OptionHelper._

import scala.language.postfixOps
import scala.xml.{Node, XML}

/**
  * ETL Configuration Parser
  */
class EtlConfigParser(xml: Node) {

  def parse: Option[EtlConfig] = {
    ((xml \\ "EtlConfig") map { rootNode =>
      val id = rootNode \@ "id"
      val layouts = parseLayouts(rootNode)
      val devices = parseDevices(rootNode, layouts)
      val flows = parseFlows(rootNode, devices)
      new EtlConfig(id, flows)
    }).headOption
  }

  private def parseDevices(rootNode: Node, layouts: Seq[Layout]): Seq[Device] = {
    (rootNode \ "devices") flatMap { devicesNode =>
      devicesNode.child.filter(_.label != "#PCDATA") map { node =>
        node.label match {
          case "KafkaOutputDevice" => parseDevices_KafkaOutputDevice(node, layouts)
          case "MongoOutputDevice" => parseDevices_MongoOutputDevice(node, layouts)
          case "MultiOutputDevice" => parseDevices_MultiOutputDevice(node, layouts)
          case "RoundRobinOutputDevice" => parseDevices_RoundRobinOutputDevice(node, layouts)
          case "TextInputDevice" => parseDevices_TextInputDevice(node, layouts)
          case "TextOutputDevice" => parseDevices_TextOutputDevice(node, layouts)
          case label =>
            throw new IllegalArgumentException(s"Invalid device type '$label'")
        }
      }
    }
  }

  private def parseDevices_KafkaOutputDevice(deviceNode: Node, layouts: Seq[Layout]) = {
    val id = deviceNode \@ "id"
    val topic = deviceNode \@ "topic"
    val connectionString = deviceNode \@ "connectionString"
    val layout = lookupOutputLayout(layouts, requiredText(deviceNode \@ "layout"))
    new KafkaOutputDevice(id, topic, ZkProxy(connectionString), layout)
  }

  private def parseDevices_MongoOutputDevice(deviceNode: Node, layouts: Seq[Layout]) = {
    val id = deviceNode \@ "id"
    val servers = deviceNode \@ "servers"
    val database = deviceNode \@ "database"
    val collection = deviceNode \@ "collection"
    val layoutId = deviceNode \@ "layout"
    val layout = lookupOutputLayout(layouts, layoutId)
    new MongoDbOutputDevice(id, servers, database, collection, layout)
  }

  private def parseDevices_MultiOutputDevice(deviceNode: Node, layouts: Seq[Layout]) = {
    val id = deviceNode \@ "id"
    val devices = parseDevices(deviceNode, layouts) map {
      case device: OutputDevice => device
      case device =>
        throw new IllegalStateException(s"${device.id} is not an output source")
    }
    val concurrency = optionalText(deviceNode \@ "concurrency").map(_.toInt) getOrElse devices.length
    new MultiOutputDevice(id, devices, concurrency)
  }

  private def parseDevices_RoundRobinOutputDevice(deviceNode: Node, layouts: Seq[Layout]) = {
    val id = deviceNode \@ "id"
    val devices = parseDevices(deviceNode, layouts) map {
      case device: OutputDevice => device
      case device =>
        throw new IllegalStateException(s"${device.id} is not an output source")
    }
    val concurrency = optionalText(deviceNode \@ "concurrency").map(_.toInt) getOrElse devices.length
    new RoundRobinOutputDevice(id, devices, concurrency)
  }

  private def parseDevices_TextInputDevice(deviceNode: Node, layouts: Seq[Layout]) = {
    val id = deviceNode \@ "id"
    val path = deviceNode \@ "path"
    val layoutId = deviceNode \@ "layout"
    val layout = lookupInputLayout(layouts, layoutId)
    new TextInputDevice(id, path, layout)
  }

  private def parseDevices_TextOutputDevice(deviceNode: Node, layouts: Seq[Layout]) = {
    val id = deviceNode \@ "id"
    val path = deviceNode \@ "path"
    val layoutId = deviceNode \@ "layout"
    val layout = lookupOutputLayout(layouts, layoutId)
    new TextOutputDevice(id, path, layout)
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

      `type` match {
        case "csv" => CSVFieldSet(fields)
        case "delimited" =>
          DelimitedFieldSet(fields, delimiter = requiredText(fieldsNode \@ "delimiter"), isQuoted = optionalText("quoted").exists(isYes))
        case "fixed-length" => FixedLengthFieldSet(fields)
        case "json" => JsonFieldSet(fields)
        case "inline" => FixedLengthFieldSet(fields)
        case unknown =>
          throw new IllegalArgumentException(s"Unrecognized fields type '$unknown'")
      }
    } headOption
  }

  private def parseFields_Field(node: Node) = {
    val name = node.text.trim
    val fixedLength = optionalText(node \@ "length") map (_.toInt)
    Field(name, fixedLength)
  }

  private def parseFlows(rootNode: Node, devices: Seq[Device]): Seq[Flow] = {
    (rootNode \ "flows") flatMap { flowsNode =>
      flowsNode.child.filter(_.label != "#PCDATA") map { node =>
        node.label match {
          case "SimpleFlow" => parseFlows_SimpleFlow(node, devices)
          case label =>
            throw new IllegalArgumentException(s"Invalid flow type '$label'")
        }
      }
    }
  }

  private def parseFlows_SimpleFlow(rootNode: Node, devices: Seq[Device]) = {
    val id = rootNode \@ "id"
    val inputId = rootNode \@ "input"
    val outputId = rootNode \@ "output"
    val input = lookupInputDevice(devices, inputId)
    val output = lookupOutputDevice(devices, outputId)
    new SimpleFlow(id, input, output)
  }

  private def parseLayouts(rootNode: Node): Seq[Layout] = {
    (rootNode \ "layouts") flatMap { layoutsNode =>
      layoutsNode.child.filter(_.label != "#PCDATA") map { node =>
        node.label match {
          case "Avro" => parseLayouts_Layout_Avro(node)
          case "Text" => parseLayouts_Layout_Text(node)
          case label =>
            throw new IllegalArgumentException(s"Invalid layout type '$label'")
        }
      }
    }
  }

  private def parseLayouts_Layout_Avro(node: Node) = {
    val id = node \@ "id"
    val fields = parseFields(node) orDie "Exactly one fields element was expected"
    val schemaString = (node \ "schema").map(_.text).headOption.orDie("Schema element required")
    new AvroLayout(id, fields, schemaString)
  }

  private def parseLayouts_Layout_Text(node: Node) = {
    val id = node \@ "id"
    val fields = parseLayouts_Layout_Body(node) orDie "Exactly one fields element was expected"
    val footer = parseLayouts_Layout_Footer(node)
    val header = parseLayouts_Layout_Header(node)
    new TextLayout(id, fields, header, footer)
  }

  private def parseLayouts_Layout_Body(rootNode: Node) = {
    for {
      node <- (rootNode \ "body").headOption
      fields <- parseFields(node)
    } yield fields
  }

  private def parseLayouts_Layout_Footer(rootNode: Node) = {
    for {
      node <- (rootNode \ "footer").headOption
      fields <- parseFields(node)
      length = optionalText(node \@ "length").map(_.toInt).getOrElse(1)
    } yield Footer(fields, length)
  }

  private def parseLayouts_Layout_Header(rootNode: Node) = {
    for {
      node <- (rootNode \ "header").headOption
      fields <- parseFields(node)
      length = optionalText(node \@ "length").map(_.toInt).getOrElse(1)
    } yield Header(fields, length)
  }

  private def lookupInputDevice(devices: Seq[Device], id: String) = {
    devices.find(_.id == id) match {
      case Some(device: InputDevice) => device
      case Some(device) =>
        throw new IllegalStateException(s"Device '${device.id}' is not an input device")
      case None =>
        throw new IllegalStateException(s"Device '$id' was not found")
    }
  }

  private def lookupInputLayout(layouts: Seq[Layout], id: String) = {
    layouts.find(_.id == id) match {
      case Some(layout: InputLayout) => layout
      case Some(layout) =>
        throw new IllegalStateException(s"Layout '$id' is not an input layout")
      case None =>
        throw new IllegalStateException(s"Layout '$id' was not found")
    }
  }

  private def lookupOutputDevice(devices: Seq[Device], id: String) = {
    devices.find(_.id == id) match {
      case Some(device: OutputDevice) => device
      case Some(device) =>
        throw new IllegalStateException(s"Device '${device.id}' is not an output device")
      case None =>
        throw new IllegalStateException(s"Device '$id' was not found")
    }
  }

  private def lookupOutputLayout(layouts: Seq[Layout], id: String) = {
    layouts.find(_.id == id) match {
      case Some(layout: OutputLayout) => layout
      case Some(layout) =>
        throw new IllegalStateException(s"Layout '$id' is not an output layout")
      case None =>
        throw new IllegalStateException(s"Layout '$id' was not found")
    }
  }

  private def isYes(s: String) = s == "y" || s == "yes" || s == "t" || s == "true"

  private def optionalText(s: String) = if (s.isEmpty) None else Some(s)

  private def requiredText(s: String) = optionalText(s).orDie(s"Required property '$s' is missing")

}

/**
  * Etl Config Parser
  */
object EtlConfigParser {

  def apply(file: File): EtlConfigParser = {
    new EtlConfigParser(XML.loadFile(file))
  }

}