package com.ldaniels528.broadway.server.datastore

import java.io.File

import com.ldaniels528.broadway.core.util.FileHelper
import com.ldaniels528.broadway.server.ServerConfig

/**
 * Data Store Module
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
class DataStore(config: ServerConfig) {

  /**
   * Moves (or copies) the given file to an archive location
   * @param file the given source [[File]]
   */
  def archive(file: File): Boolean = FileHelper.archive(file, config.getArchiveDirectory)

}