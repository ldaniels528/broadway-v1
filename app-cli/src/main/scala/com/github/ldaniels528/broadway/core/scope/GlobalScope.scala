package com.github.ldaniels528.broadway.core.scope

import com.github.ldaniels528.broadway.core.io.device.Device
import com.github.ldaniels528.broadway.core.io.device.text.{TextFileInputDevice, TextFileOutputDevice}
import com.github.ldaniels528.broadway.core.opcode.flow.Flow
import com.github.ldaniels528.broadway.core.scope.Scope.ScopeFunction
import com.ldaniels528.commons.helpers.OptionHelper._

import scala.collection.concurrent.TrieMap

/**
  * Global Scope
  */
case class GlobalScope(flow: Flow) extends Scope {
  private val dynamicValues = buildDynamicValues()
  private val mutableValues = TrieMap[String, Any]()
  private val handles = TrieMap[String, Any]()

  override def add(name: String, value: Any) = {
    mutableValues.put(name, value)
    ()
  }

  override def find(name: String, property: String): Option[ScopeFunction] = {
    findMutableValue(name, property) ?? findDynamicValue(name, property)
  }

  override def getReader[T] = handles("READER").asInstanceOf[T]

  override def getWriter[T] = handles("WRITER").asInstanceOf[T]

  override def openReader[T](action: => T) = handles.getOrElseUpdate("READER", action).asInstanceOf[T]

  override def openWriter[T](action: => T) = handles.getOrElseUpdate("WRITER", action).asInstanceOf[T]

  private def findMutableValue(name: String, property: String): Option[ScopeFunction] = {
    mutableValues.get(s"$name.$property") map { value =>
      (scope: Scope) => value
    }
  }

  private def findDynamicValue(name: String, property: String): Option[ScopeFunction] = {
    for {
      obj <- dynamicValues.get(name)
      value <- obj.get(property)
    } yield value
  }

  private def buildDynamicValues(): TrieMap[String, Map[String, ScopeFunction]] = {
    val mapping = TrieMap[String, Map[String, ScopeFunction]]()

    // define all devices
    flow.devices foreach { device =>
      mapping.put(device.id, Map(
        "id" -> ((scope: Scope) => device.id),
        "count" -> ((scope: Scope) => device.count),
        "offset" -> ((scope: Scope) => device.offset)
      ))
    }

    // define the primary input device
    mapping.put("__INPUT", Map(
      "id" -> ((scope: Scope) => flow.input.id),
      "count" -> ((scope: Scope) => flow.input.count),
      "offset" -> ((scope: Scope) => flow.input.offset),
      "path" -> ((scope: Scope) => getPath(flow.input))
    ))

    // define the primary output device
    mapping.put("__OUTPUT", Map(
      "id" -> ((scope: Scope) => flow.output.id),
      "count" -> ((scope: Scope) => flow.output.count),
      "offset" -> ((scope: Scope) => flow.output.offset),
      "path" -> ((scope: Scope) => getPath(flow.output))
    ))

    mapping
  }

  private def getPath(device: Device): String = {
    device match {
      case d: TextFileInputDevice => d.path
      case d: TextFileOutputDevice => d.path
      case _ => ""
    }
  }

}


