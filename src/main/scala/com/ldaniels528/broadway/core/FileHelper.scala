package com.ldaniels528.broadway.core

import java.io.{File, FileInputStream, FileOutputStream}
import java.util.zip.GZIPOutputStream

import com.ldaniels528.trifecta.util.ResourceHelper._
import org.apache.commons.io.IOUtils
import org.slf4j.LoggerFactory

/**
 * File Helper Utility class
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
object FileHelper {
  private lazy val logger = LoggerFactory.getLogger(getClass)

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

}
