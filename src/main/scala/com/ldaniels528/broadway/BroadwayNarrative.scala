package com.ldaniels528.broadway

import akka.actor.Actor
import com.ldaniels528.broadway.core.resources._
import com.ldaniels528.broadway.server.ServerConfig

import scala.concurrent.ExecutionContext
import scala.language.implicitConversions
import scala.reflect.ClassTag

/**
 * This class describes a narrative; or flow for a given data process
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
case class BroadwayNarrative(config: ServerConfig, name: String) {
  protected implicit val executionContext = config.system.dispatcher
  private var executable: Option[Resource => Unit] = None

  /**
   * Adds a new actor to the narrative
   * @param actor the given [[Actor]]
   * @param parallelism the number of actors to create
   * @tparam T the actor type
   * @return an actor reference
   */
  def addActor[T <: Actor : ClassTag](actor: => T, parallelism: Int = 1) = config.addActor(actor, parallelism)

  /**
   * Setups the actions that will occur upon start of the topology
   * @param block the executable block
   */
  def onStart(block: Resource => Unit)(implicit ec: ExecutionContext) = {
    executable = Option(block)
  }

  /**
   * Starts executing the topology
   */
  def start(resource: Resource) {
    executable.foreach(_(resource))
  }

}
