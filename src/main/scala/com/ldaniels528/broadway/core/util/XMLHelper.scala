package com.ldaniels528.broadway.core.util

import com.ldaniels528.trifecta.util.OptionHelper._

import scala.xml.Node

/**
 * Scala XML Helper Utility
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
object XMLHelper {

  /**
   * Syntactic sugar and convenience methods for Node instances
   * @param node the given [[Node]]
   */
  implicit class NodeExtensions(val node: Node) extends AnyVal {

    def getAttr(tagName: String, name: String) = {
      (node \ s"@$name").headOption map (_.text) orDie s"Required attribute '$name' not found in element $tagName"
    }

    def getAttrOpt(name: String) = (node \ s"@$name").headOption map (_.text)

    def getText(name: String) = (node \ name).headOption map (_.text)

  }

}
