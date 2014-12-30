package com.ldaniels528.broadway.server

import java.io.File

import com.ldaniels528.broadway.core.FileHelper._
import com.ldaniels528.broadway.core.FileHelper.logger
import com.ldaniels528.broadway.core.Resources.{ReadableResource, ClasspathResource}
import com.ldaniels528.broadway.server.ServerConfig._
import com.ldaniels528.broadway.server.ServerConfig.logger
import com.ldaniels528.trifecta.util.PropertiesHelper._
import com.shocktrade.topologies.NASDAQDataImportTopology._
import org.slf4j.LoggerFactory

import scala.util.{Failure, Success, Try}

/**
 * Server Config
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
class ServerConfig(props: java.util.Properties) {

  def getRootDirectory = new File(new File(props.getOrElse(BaseDir, scala.util.Properties.userHome)), "broadway")

  def getArchiveDirectory = new File(getRootDirectory, "archive")

  def getCompletedDirectory = new File(getRootDirectory, "completed")

  def getFailedDirectory = new File(getRootDirectory, "failed")

  def getIncomingDirectory = new File(getRootDirectory, "incoming")

  def getWorkDirectory = new File(getRootDirectory, "work")

  /**
   * Initializes the environment based on this configuration
   */
  def init(): Unit = {
    // ensure that all processing directories exist
    Seq(
      getArchiveDirectory, getCompletedDirectory,
      getFailedDirectory, getIncomingDirectory, getWorkDirectory) foreach ensureExistence
  }

}

/**
 * Server Config Singleton
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
object ServerConfig {
  private lazy val logger = LoggerFactory.getLogger(getClass)
  private val BaseDir = "broadway.directories.base"

  /**
   * Loads the default server configuration
   * @return the default server configuration
   */
  def apply() = loadConfig(ClasspathResource("/server-config.properties"))

  /**
   * Loads the server configuration from the given resource
   * @return the server configuration
   */
  def loadConfig(resource: ReadableResource) = {
    val props = new java.util.Properties()
    Try(resource.getInputStream foreach props.load) match {
      case Success(_) =>
      case Failure(e) =>
        logger.error(s"Error loading configuration from '$resource'", e)
    }
    new ServerConfig(props)
  }

}