package com.ldaniels528.broadway.server.etl

import java.io.File

import com.ldaniels528.broadway.core.Resources.FileResource
import com.ldaniels528.broadway.server.ServerConfig
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}

/**
 * ETL Processor Application
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
class ETLProcessor(config: ServerConfig)(implicit ec: ExecutionContext) {
  private lazy val logger = LoggerFactory.getLogger(getClass)

  def execute(topology: BroadwayTopology, file: File) = Future {
    val name = topology.name
    logger.info(s"$name: Processing '${file.getAbsolutePath}'....")
    val start = System.currentTimeMillis()
    topology.start(FileResource(file.getAbsolutePath))
    logger.info(s"$name: Completed in ${System.currentTimeMillis() - start} msec")
  }

}
