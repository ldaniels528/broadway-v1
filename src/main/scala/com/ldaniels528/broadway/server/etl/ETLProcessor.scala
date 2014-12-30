package com.ldaniels528.broadway.server.etl

import java.io.File

import com.ldaniels528.broadway.core.Resources.{ClasspathResource, FileResource}
import com.ldaniels528.broadway.server.ServerConfig
import com.ldaniels528.broadway.server.etl.ETLProcessor.ETLProcess
import com.ldaniels528.trifecta.util.OptionHelper._
import com.shocktrade.topologies.NASDAQDataImportTopology
import org.slf4j.LoggerFactory

import scala.collection.concurrent.TrieMap
import scala.concurrent.{ExecutionContext, Future}

/**
 * ETL Processing Module
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
class ETLProcessor(config: ServerConfig) {
  private val mappings = TrieMap(
    Seq("AMEX.txt", "NASDAQ.txt", "NYSE.txt", "OTCBB.txt")
      .map(name => (name, ETLProcess(NASDAQDataImportTopology.createTopology(name))))
      : _*)

  def getMapping(name: String) = mappings.get(name)

}

/**
 * ETL Processor Application
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
object ETLProcessor {
  private lazy val logger = LoggerFactory.getLogger(getClass)

  /**
   * Enables command line execution
   * {{{ emit.sh nasdaqFileFlow.xml --input-source "/data/AMEX.txt" }}}
   * @param args the given command line arguments
   */
  def main(args: Array[String]): Unit = execute(args)

  /**
   * Executes the topology
   * @param args the given command line arguments
   */
  def execute(args: Array[String]) = {

  }

  private def getResource(resourcePath: String) = {
    FileResource(resourcePath).getInputStream ?? ClasspathResource(resourcePath).getInputStream
  }

  /**
   * Represents an ETL process
   * @param topology the given [[BroadwayTopology]]
   */
  case class ETLProcess(topology: BroadwayTopology) {

    /**
     * Returns the name of the topology
     * @return the name of the topology
     */
    def name = topology.name

    /**
     * Processes the given file
     * @param file the given file
     */
    def execute(file: File)(implicit ec: ExecutionContext) = Future {
      logger.info(s"$name: Processing '${file.getAbsolutePath}'....")
      val start = System.currentTimeMillis()
      topology.start(FileResource(file.getAbsolutePath))
      logger.info(s"$name: Completed in ${System.currentTimeMillis() - start} msec")
    }

  }

}
