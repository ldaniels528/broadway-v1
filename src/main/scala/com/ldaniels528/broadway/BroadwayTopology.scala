package com.ldaniels528.broadway

import com.ldaniels528.broadway.core.resources._
import com.ldaniels528.broadway.server.ServerConfig
import org.slf4j.LoggerFactory

import scala.language.implicitConversions

/**
 * Broadway Topology
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
class BroadwayTopology(val config: ServerConfig, val name: String) {
  private lazy val logger = LoggerFactory.getLogger(getClass)
  private implicit val executionContext = config.system.dispatcher
  private var executable: Option[ReadableResource => Unit] = None

  /**
   * Setups the actions that will occur upon start of the topology
   * @param block the executable block
   */
  def onStart(block: ReadableResource => Unit) {
    this.executable = Option(block)
  }

  /**
   * Starts executing the topology
   */
  def start(resource: ReadableResource) {
    // capture the start time
    val startTime = System.currentTimeMillis()

    // execute the topology
    executable.foreach(_(resource))

    // report the total runtime in milliseconds to the operator
    val elapsedTime = System.currentTimeMillis() - startTime
    logger.info(s"Processed '$name' in $elapsedTime msec")
  }

}
