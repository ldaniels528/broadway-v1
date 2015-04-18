package com.ldaniels528.broadway.core.util

import java.io.{File, FileInputStream, FileOutputStream}
import java.text.SimpleDateFormat
import java.util.Date
import java.util.zip.GZIPOutputStream

import com.ldaniels528.commons.helpers.ResourceHelper._
import org.apache.commons.io.IOUtils
import org.slf4j.LoggerFactory

/**
 * File Helper Utility class
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
object FileHelper {
  private lazy val logger = LoggerFactory.getLogger(getClass)
  private val dateFormat = "yyyyMMdd"
  private val timeFormat = "hhmmss"

  /**
   * Moves (or copies) the given file to an archive location
   * @param file the given source [[File]]
   */
  def archive(file: File, archiveDirectory: File): Boolean = {
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
   * Copies the original file to the new file location
   * @param file the original file
   * @param newFile the new file location
   * @return true, if the file was successfully copied
   */
  def copyFile(file: File, newFile: File): Boolean = {
    val count = new FileInputStream(file) use (in =>
      new FileOutputStream(newFile) use (out => IOUtils.copyLarge(in, out)))
    count == file.length()
  }

  /**
   * Copies the original file to the new file location
   * @param file the original file
   * @param newFile the new file location
   * @return true, if the file was successfully copied
   */
  def copyFileWithCompression(file: File, newFile: File): Boolean = {
    val count = new FileInputStream(file) use (in =>
      new GZIPOutputStream(new FileOutputStream(newFile)) use (out => IOUtils.copyLarge(in, out)))
    count > 0
  }

  /**
   * Ensures that the given directory exists, and attempts to create it if it doesn't
   * @param directory the given directory
   */
  @throws[IllegalStateException]
  def ensureExistence(directory: File) {
    if (!directory.exists() && !directory.mkdirs() && !directory.exists()) {
      throw new IllegalStateException(s"Unable to create directory (${directory.getAbsolutePath})")
    }
  }

  /**
   * Determines whether the given file is GZIP compressed
   * @param file the given [[File]]
   * @return true, if the filename ends with ".tgz" or ".gz"
   */
  def isCompressed(file: File) = {
    val name = file.getName.toLowerCase
    name.endsWith(".tgz") || name.endsWith(".gz")
  }

  /**
   * Moves the given file to the specified location
   * @param oldFile the given source [[File]]
   * @param newFile the given destination [[File]]
   */
  def move(oldFile: File, newFile: File): Boolean = {
    // attempt to move the file locally
    if (oldFile.renameTo(newFile)) {
      logger.info(s"Successfully moved local file ${oldFile.getName} to ${newFile.getParentFile.getAbsolutePath}")
      true
    } else {
      val copied = copyFile(oldFile, newFile)
      if (copied) {
        logger.info(s"Successfully moved ${oldFile.getName} to ${newFile.getParentFile.getAbsolutePath}")
        if (oldFile.delete()) {
          logger.info(s"Deleting original file ${oldFile.getAbsolutePath}")
        }
        else {
          logger.warn(s"Unable to delete original file ${oldFile.getAbsolutePath}")
        }
      }
      copied
    }
  }

  /**
   * Formats the current date as a string using the specified custom date/time format
   * @param format the specified custom date/time format
   * @return the formatted date/time string
   */
  private def formatDate(format: String) = new SimpleDateFormat(format).format(new Date())

}
