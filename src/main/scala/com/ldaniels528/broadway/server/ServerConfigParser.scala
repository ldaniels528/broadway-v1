package com.ldaniels528.broadway.server

import com.ldaniels528.broadway.core.resources._
import com.ldaniels528.broadway.core.triggers.schedules.Scheduling
import com.ldaniels528.broadway.core.util.XMLHelper._
import com.ldaniels528.broadway.server.ServerConfig.HttpInfo
import com.ldaniels528.commons.helpers.PropertiesHelper._

import scala.util.{Failure, Success, Try}
import scala.xml._

/**
 * Server Configuration Parser
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
object ServerConfigParser {

  /**
   * Attempts to extract a server configuration from the given resource
   * @param resource he given [[ReadableResource]]
   * @return an [[Option]] of a [[ServerConfig]]
   */
  def parse(resource: ReadableResource): Option[ServerConfig] = {
    resource.getInputStream map { in =>
      val xml = XML.load(in)
      new ServerConfig(parseDirectories(xml), parseHttpInfo(xml))
    }
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

}
