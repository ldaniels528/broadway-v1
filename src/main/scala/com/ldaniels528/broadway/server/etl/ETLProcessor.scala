package com.ldaniels528.broadway.server.etl

import java.io.File

import com.ldaniels528.broadway.core.Resources.FileResource
import com.ldaniels528.broadway.server.ServerConfig
import com.ldaniels528.broadway.server.etl.ETLProcessor._
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

/**
 * ETL Processing Module
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
class ETLProcessor(config: ServerConfig) {
  private lazy val logger = LoggerFactory.getLogger(getClass)
  private val processors = createETLProcessor()

  /**
   * Retrieves the processor that is mapped to the given expression
   * @param expression the given expression; this could be a direct processor name or regular expression
   * @return an [[Option]] of a tuple of a [[com.ldaniels528.broadway.server.ServerConfig.Feed]] and an [[ETLProcess]]
   */
  def getProcessor(expression: String) = processors.find { case (feed, process) => feed.matches(expression)}

  private def createETLProcessor() = {
    config.topologies flatMap { t =>
      instantiateTopology(t.className) match {
        case Success(topology) =>
          t.feeds.map(f => f -> ETLProcess(topology))
        case Failure(e) =>
          logger.info(s"Error initializing topology class '${t.className}'")
          Nil
      }
    }
  }

  private def instantiateTopology(className: String) = Try {
    Class.forName(className).newInstance().asInstanceOf[BroadwayTopology]
  }

}

/**
 * ETL Processor Application
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
object ETLProcessor {

  /**
   * Represents an ETL process
   * @param topology the given [[BroadwayTopology]]
   */
  case class ETLProcess(topology: BroadwayTopology) {
    private lazy val logger = LoggerFactory.getLogger(getClass)

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
