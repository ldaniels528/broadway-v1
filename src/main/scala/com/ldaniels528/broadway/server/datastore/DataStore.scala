package com.ldaniels528.broadway.server.datastore

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

import com.ldaniels528.broadway.core.FileHelper._
import com.ldaniels528.broadway.server.ServerConfig
import org.slf4j.LoggerFactory

/**
 * Data Store Module
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
class DataStore(config: ServerConfig) {
  private lazy val logger = LoggerFactory.getLogger(getClass)
  private val archiveDirectory = config.getArchiveDirectory
  private val dateFormat = "yyyyMMdd"
  private val timeFormat = "hhmmss"

  /**
   * Moves (or copies) the given file to an archive location
   * @param file the given source [[File]]
   */
  def archive(file: File): Boolean = {
    logger.info(s"Archiving file ${file.getAbsolutePath}")
    if (!file.exists) {
      logger.warn(s"File '${file.getAbsolutePath}' does not exist")
      false
    }
    else {
      // create the target path for the file
      //val compressed = isCompressed(file)
      val parentFile = new File(new File(archiveDirectory, formatDate(dateFormat)), formatDate(timeFormat)).getCanonicalFile
      val newFile = new File(parentFile, file.getName)
      ensureExistence(newFile.getParentFile)

      // attempt to move the file locally
      if (file.renameTo(newFile)) {
        logger.info(s"Successfully moved local file ${file.getName} to ${parentFile.getAbsolutePath}")
        true
      } else {
        val copied = copyFile(file, newFile)
        if (copied) {
          logger.info(s"Successfully moved ${file.getName} to ${parentFile.getAbsolutePath}")
          if (file.delete()) {
            logger.info(s"Deleting original file ${file.getAbsolutePath}")
          }
          else {
            logger.warn(s"Unable to delete original file ${file.getAbsolutePath}")
          }
        }
        copied
      }
    }
  }

  /**
   * Formats the current date as a string using the specified custom date/time format
   * @param format the specified custom date/time format
   * @return the formatted date/time string
   */
  private def formatDate(format: String) = new SimpleDateFormat(format).format(new Date())

}