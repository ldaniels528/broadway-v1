package com.github.ldaniels528.broadway.core

import java.io.File

import com.github.ldaniels528.broadway.core.XMLConfigParser.SequenceEnrichment
import com.github.ldaniels528.broadway.core.flow.{Flow, SimpleFlow}
import com.github.ldaniels528.broadway.core.io.device._
import com.github.ldaniels528.broadway.core.io.device.kafka.{KafkaOutputDevice, ZkProxy}
import com.github.ldaniels528.broadway.core.io.device.nosql.MongoDbOutputDevice
import com.github.ldaniels528.broadway.core.io.device.text.{TextInputDevice, TextOutputDevice}
import com.github.ldaniels528.broadway.core.io.layout._
import com.github.ldaniels528.broadway.core.io.layout.avro.AvroLayout
import com.github.ldaniels528.broadway.core.io.layout.text.TextLayout
import com.github.ldaniels528.broadway.core.io.layout.text.fields._
import com.ldaniels528.commons.helpers.OptionHelper._

import scala.language.postfixOps
import scala.xml.{Node, XML}

/**
  * ETL Configuration Parser
  */
class XMLConfigParser(xml: Node) {

  def parse: Option[ETLConfig] = {
    ((xml \\ "etl-config") map { node =>
      ETLConfig(
        id = node \@ "id",
        flows = parseFlows(
          rootNode = node,
          devices = parseDevices(node, layouts = parseLayouts(node))))
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
    KafkaOutputDevice(
      id = deviceNode \@ "id",
      topic = deviceNode \@ "topic",
      zk = ZkProxy(connectionString = deviceNode \@ "connectionString"),
      layout = lookupOutputLayout(layouts, requiredText(deviceNode \@ "layout")))
  }

  private def parseDevices_MongoOutputDevice(deviceNode: Node, layouts: Seq[Layout]) = {
    MongoDbOutputDevice(
      id = deviceNode \@ "id",
      serverList = deviceNode \@ "servers",
      database = deviceNode \@ "database",
      collection = deviceNode \@ "collection",
      layout = lookupOutputLayout(layouts, id = deviceNode \@ "layout"))
  }

  private def parseDevices_MultiOutputDevice(deviceNode: Node, layouts: Seq[Layout]) = {
    MultiOutputDevice(
      id = deviceNode \@ "id",
      devices = parseDevices(deviceNode, layouts).only[OutputDevice](_.id, "output device"),
      concurrency = optionalText(deviceNode \@ "concurrency").map(_.toInt) getOrElse 2)
  }

  private def parseDevices_RoundRobinOutputDevice(deviceNode: Node, layouts: Seq[Layout]) = {
    RoundRobinOutputDevice(
      id = deviceNode \@ "id",
      devices = parseDevices(deviceNode, layouts).only[OutputDevice](_.id, "output device"),
      concurrency = optionalText(deviceNode \@ "concurrency").map(_.toInt) getOrElse 2)
  }

  private def parseDevices_TextInputDevice(deviceNode: Node, layouts: Seq[Layout]) = {
    TextInputDevice(
      id = deviceNode \@ "id",
      path = deviceNode \@ "path",
      layout = lookupInputLayout(layouts, id = deviceNode \@ "layout"))
  }

  private def parseDevices_TextOutputDevice(deviceNode: Node, layouts: Seq[Layout]) = {
    TextOutputDevice(
      id = deviceNode \@ "id",
      path = deviceNode \@ "path",
      layout = lookupOutputLayout(layouts, id = deviceNode \@ "layout"))
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
          DelimitedFieldSet(fields, delimiter = requiredText(fieldsNode \@ "delimiter"), isQuoted = optionalText("quoted").exists(isYes))
        case "fixed-length" => FixedLengthFieldSet(fields)
        case "json" => JsonFieldSet(fields)
        case "inline" => FixedLengthFieldSet(fields)
        case unknown =>
          throw new IllegalArgumentException(s"Unrecognized fields type '$unknown'")
      }
    }
  }

  private def parseFields_Field(node: Node) = {
    Field(
      name = node.text.trim,
      length = optionalText(node \@ "length") map (_.toInt))
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
    SimpleFlow(
      id = rootNode \@ "id",
      input = lookupInputDevice(devices, id = rootNode \@ "input"),
      output = lookupOutputDevice(devices, id = rootNode \@ "output"))
  }

  private def parseLayouts(rootNode: Node): Seq[Layout] = {
    (rootNode \ "layouts") flatMap { layoutsNode =>
      layoutsNode.child.filter(_.label != "#PCDATA") map { node =>
        node.label match {
          case "AvroLayout" => parseLayouts_Layout_Avro(node)
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
      case Some(node) => parseFields(node) map (Division(_))
      case None => Nil
    }
  }

  private def parseLayouts_Layout_Text(node: Node) = {
    TextLayout(
      id = node \@ "id",
      header = parseLayouts_Layout_Division(node, "header"),
      body = parseLayouts_Layout_Division(node, "body"),
      footer = parseLayouts_Layout_Division(node, "footer"))
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
object XMLConfigParser {

  def apply(file: File) = new XMLConfigParser(XML.loadFile(file))

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