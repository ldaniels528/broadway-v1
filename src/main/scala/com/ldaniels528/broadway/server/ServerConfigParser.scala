package com.ldaniels528.broadway.server

import com.ldaniels528.broadway.core.Resources.ReadableResource

import scala.xml._

/**
 * Server Configuration Parser
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
object ServerConfigParser {

  def parse(resource: ReadableResource) = {
    resource.getInputStream map { in =>
      val xml = XML.load(in)
      parseDirectories(xml)
    }
  }

  private def parseDirectories(doc: Node) = {
    (doc \ "directories") map { node =>
      node.get("archive")

    }
  }

  implicit class NodeExtensions(val node: Node) extends AnyVal {

    def get(name: String) = (node \ "archive").headOption map (_.text)

  }

}
