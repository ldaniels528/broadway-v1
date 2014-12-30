package com.ldaniels528.broadway.server.transporter

import java.io.File

import com.ldaniels528.broadway.core.FileHelper._
import com.ldaniels528.broadway.server.ServerConfig
import com.ldaniels528.broadway.server.etl.ETLProcessor
import com.ldaniels528.broadway.server.etl.ETLProcessor.ETLProcess
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

/**
 * Data Transporter Module
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
class DataTransporter(config: ServerConfig)(implicit ec: ExecutionContext) {
  private lazy val logger = LoggerFactory.getLogger(getClass)
  private val etl = new ETLProcessor(config)

  /**
   * Processes the given file
   * @param file the given file
   */
  def process(file: File) {
    val fileName = file.getName
    etl.getMapping(fileName) match {
      case Some(process) => processETL(process, file)
      case None => noMappedProcess(file)
    }
    ()
  }

  /**
   * Processes the given file via an ETL process
   * @param process the given [[ETLProcess]]
   * @param file the given [[File]]
   */
  private def processETL(process: ETLProcess, file: File) = {
    val fileName = file.getName
    logger.info(s"${process.name}: Moving file '$fileName' to '${config.getWorkDirectory}' for processing...")
    val wipFile = new File(config.getWorkDirectory, fileName)
    move(file, wipFile)
    process.execute(wipFile) onComplete {
      case Success(result) =>
        move(wipFile, new File(config.getCompletedDirectory, fileName))
      case Failure(e) =>
        logger.error(s"${process.name}: File '$fileName' failed during processing", e)
        move(wipFile, new File(config.getFailedDirectory, fileName))
    }
  }

  /**
   * Called when no mapping process is found for the given file
   * @param file the given [[File]]
   */
  private def noMappedProcess(file: File) = {
    val fileName = file.getName
    logger.info(s"No processing mappings found for '$fileName'. Moving to '${config.getCompletedDirectory}' for archival...")
    move(file, new File(config.getCompletedDirectory, fileName))
  }

}
