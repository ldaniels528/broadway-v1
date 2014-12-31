package com.ldaniels528.broadway.server

import com.ldaniels528.broadway.core.Resources.ReadableResource
import com.ldaniels528.broadway.server.ServerConfig.{Topology, HttpInfo, Feed}
import com.ldaniels528.trifecta.util.OptionHelper._
import com.ldaniels528.trifecta.util.PropertiesHelper._

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
      new ServerConfig(
        parseDirectories(xml),
        parseHttp(xml),
        parseTopologies(xml))
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
  private def parseHttp(doc: Node) = {
    ((doc \ "http") map { node =>
      val host = (node \ "host").text
      val port = (node \ "port").text
      HttpInfo(host, port.toInt)
    }).headOption
  }

  /**
   * Parses the <code>topology</code> tags
   * @param doc the given XML node
   * @return a [[Seq]] of [[Topology]]
   */
  private def parseTopologies(doc: Node) = {
    (doc \ "topology") map { node =>
      val className = node.getAttr("class")
      val feeds = (node \ "feed") map { feedNode =>
        val name = feedNode.text
        val matching = feedNode.getAttr("match")
        Feed(name, matching)
      }
      Topology(className, feeds)
    }
  }

  /**
   * Syntactic sugar and convenience methods for Node instances
   * @param node the given [[Node]]
   */
  implicit class NodeExtensions(val node: Node) extends AnyVal {

    def getAttr(name: String) = (node \ s"@$name").headOption map (_.text) orDie s"Required attribute '$name' not found"

    def getText(name: String) = (node \ name).headOption map (_.text)

  }

}
