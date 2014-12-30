package com.ldaniels528.broadway.server

import java.io.File

import com.ldaniels528.broadway.core.FileHelper._

/**
 * Server Config
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
class ServerConfig() {

  def getRootDirectory = new File(new File(scala.util.Properties.userHome), "flow")

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

  /**
   * Loads the default server configuration
   * @return the default configuration
   */
  def loadConfig() = new ServerConfig()

}