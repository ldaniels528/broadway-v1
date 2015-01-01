package com.ldaniels528.broadway.server

import java.io.File

import com.ldaniels528.broadway.core.FileHelper._
import com.ldaniels528.broadway.core.Resources.{ClasspathResource, ReadableResource}
import com.ldaniels528.broadway.server.ServerConfig._
import com.ldaniels528.trifecta.util.PropertiesHelper._

/**
 * Server Config
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
case class ServerConfig(props: java.util.Properties,
                        httpInfo: Option[HttpInfo],
                        topologies: Seq[Topology]) {

  def getRootDirectory = new File(new File(props.getOrElse(BaseDir, scala.util.Properties.userHome)), "broadway")

  def getArchiveDirectory = new File(getRootDirectory, "archive")

  def getCompletedDirectory = new File(getRootDirectory, "completed")

  def getFailedDirectory = new File(getRootDirectory, "failed")

  def getIncomingDirectory = new File(getRootDirectory, "incoming")

  def getTopologiesDirectory = new File(getRootDirectory, "topologies")

  def getWorkDirectory = new File(getRootDirectory, "work")

  /**
   * Initializes the environment based on this configuration
   */
  def init() = Seq(
    getArchiveDirectory, getCompletedDirectory, getFailedDirectory,
    getIncomingDirectory, getTopologiesDirectory, getWorkDirectory) foreach ensureExistence

}

/**
 * Server Config Singleton
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
object ServerConfig {
  private val BaseDir = "broadway.directories.base"

  /**
   * Loads the default server configuration
   * @return the default server configuration
   */
  def apply(): Option[ServerConfig] = apply(ClasspathResource("/broadway-config.xml"))

  /**
   * Loads the server configuration from the given resource
   * @return the server configuration
   */
  def apply(resource: ReadableResource): Option[ServerConfig] = ServerConfigParser.parse(resource)

  case class FeedSet() {

  }

  case class Feed(name: String, matching: String) {
    def matches(text: String): Boolean = {
      matching match {
        case "exact" => name == text
        case "regex" => name.matches(text)
        case "start" => name.startsWith(text)
        case "ends" => name.endsWith(text)
        case _ => name == text
      }
    }
  }

  case class Location(feeds: Seq[Feed]) {

    def findFeed(name: String): Option[Feed] = feeds.find(_.matches(name))

  }

  case class HttpInfo(host: String, port: Int)

  case class Topology(className: String, feeds: Seq[Feed])

}