package com.ldaniels528.broadway.server.transporter

import java.io.File

import com.ldaniels528.broadway.core.util.FileHelper
import FileHelper._
import com.ldaniels528.broadway.core.topology.{Feed, Location, TopologyRuntime}
import com.ldaniels528.broadway.server.ServerConfig
import com.ldaniels528.broadway.server.etl.ETLProcessor
import org.slf4j.LoggerFactory

import scala.collection.concurrent.TrieMap
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

/**
 * Data Transporter Module
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
class DataTransporter(config: ServerConfig)(implicit ec: ExecutionContext) {
  private lazy val logger = LoggerFactory.getLogger(getClass)
  private implicit val rt = new TopologyRuntime()
  private val reported = TrieMap[String, Throwable]()
  private val etl = new ETLProcessor(config)

  /**
   * Processes the given file
   * @param file the given file
   */
  def process(location: Location, file: File) {
    location.findFeed(file.getName) match {
      case Some(feed) => processETL(feed, file)
      case None => noMappedProcess(location, file)
    }
    ()
  }

  /**
   * Processes the given file via an ETL process
   * @param feed the given [[Feed]]
   * @param file the given [[File]]
   */
  private def processETL(feed: Feed, file: File) = {
    feed.topology foreach { td =>
      // lookup the topology
      rt.getTopology(td) match {
        case Success(topology) =>
          val fileName = file.getName
          logger.info(s"${topology.name}: Moving file '$fileName' to '${config.getWorkDirectory}' for processing...")
          val wipFile = new File(config.getWorkDirectory, fileName)
          move(file, wipFile)

          // start processing
          etl.execute(topology, wipFile) onComplete {
            case Success(result) =>
              move(wipFile, new File(config.getCompletedDirectory, fileName))
            case Failure(e) =>
              logger.error(s"${topology.name}: File '$fileName' failed during processing", e)
              move(wipFile, new File(config.getFailedDirectory, fileName))
          }
        case Failure(e) =>
          if(!reported.contains(td.id)) {
            logger.error(s"${td.id}: Topology could not be instantiated", e)
            reported += td.id -> e
          }
      }

    }
  }

  /**
   * Called when no mapping process is found for the given file
   * @param file the given [[File]]
   */
  private def noMappedProcess(location: Location, file: File) = {
    val fileName = file.getName
    logger.info(s"${location.id}: No mappings found for '$fileName'. Moving to '${config.getCompletedDirectory}' for archival.")
    move(file, new File(config.getCompletedDirectory, fileName))
  }

}
