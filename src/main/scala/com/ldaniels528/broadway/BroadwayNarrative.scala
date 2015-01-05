package com.ldaniels528.broadway

import com.ldaniels528.broadway.core.resources._
import com.ldaniels528.broadway.server.ServerConfig

import scala.concurrent.ExecutionContext
import scala.language.implicitConversions

/**
 * This class describes a narrative; or flow for a given data process
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
class BroadwayNarrative(val config: ServerConfig, val name: String) {
  protected implicit val executionContext = config.system.dispatcher
  private var executable: Option[ReadableResource => Unit] = None

  /**
   * Setups the actions that will occur upon start of the topology
   * @param block the executable block
   */
  def onStart(block: ReadableResource => Unit)(implicit ec: ExecutionContext) = executable = Option(block)

  /**
   * Starts executing the topology
   */
  def start(resource: ReadableResource) = executable.foreach(_(resource))

}
