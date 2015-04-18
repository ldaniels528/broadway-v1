package com.ldaniels528.broadway

import java.util.Properties

import akka.actor.Actor
import com.datastax.driver.core.utils.UUIDs
import com.ldaniels528.broadway.core.resources._
import com.ldaniels528.broadway.server.ServerConfig
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration
import scala.language.implicitConversions
import scala.reflect.ClassTag

/**
 * This class describes a narrative; or flow for a given data process
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
case class BroadwayNarrative(config: ServerConfig, name: String, props: Properties) {
  protected implicit val executionContext = config.system.dispatcher
  private var setup: Option[Option[Resource] => Unit] = None
  private var tearDown: Option[() => Unit] = None
  val log = LoggerFactory.getLogger(getClass)

  /**
   * Facilitates the execution of the block on a specific interval
   * @param duration the given [[Duration]]
   * @param block the given code block
   */
  def every(duration: Duration)(block: => Unit) {
    block
  }

  /**
   * Setups the actions that will occur upon start of the narrative
   * @param block the executable block
   */
  def onStart(block: Option[Resource] => Unit)(implicit ec: ExecutionContext) = {
    setup = Option(block)
  }

  /**
   * Tears down the actions that were setup at the start of the narrative
   * @param block the executable block
   */
  def onStop(block: () => Unit)(implicit ec: ExecutionContext) = {
    tearDown = Option(block)
  }

  /**
   * Prepares a new actor for execution within the narrative
   * @param id the given unique identifier
   * @param actor the given [[Actor]]
   * @param parallelism the number of actors to create
   * @tparam T the actor type
   * @return an actor reference
   */
  def prepareActor[T <: Actor : ClassTag](actor: => T, parallelism: Int = 1, id: String = randomName) = {
    config.prepareActor(id, actor, parallelism)
  }

  /**
   * Starts executing the narrative
   */
  def start(resource: Option[Resource]): Unit = setup.foreach(_(resource))

  /**
   * Stops the execution of the narrative
   */
  def stop(): Unit = tearDown.foreach(_())

  /**
   * Returns a random actor name
   * @return a random actor name (e.g. "actor_a2111_345_23000")
   */
  private def randomName: String = s"actor_${UUIDs.timeBased}"

}
