package com.ldaniels528.broadway.server.etl

import akka.actor.{Actor, ActorSystem, Props}
import com.ldaniels528.broadway.core.resources._
import org.slf4j.LoggerFactory

import scala.reflect.ClassTag

/**
 * Broadway Topology
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
class BroadwayTopology(val name: String) {
  private lazy val logger = LoggerFactory.getLogger(getClass)
  private var executable: Option[ReadableResource => Unit] = None
  lazy val system = ActorSystem("BroadwaySystem_2")

  implicit val executionContext = system.dispatcher

  /**
   * Adds a new actor to the topology
   * @param actor the given [[Actor]]
   * @return an [[akka.actor.ActorRef]]
   */
  def addActor[T <: Actor : ClassTag](actor: => T) = system.actorOf(Props(actor))

  /**
   * Setups the actions that will occur upon start of the topology
   * @param block the executable block
   */
  def onStart(block: ReadableResource => Unit): Unit = {
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
    system.awaitTermination()
    logger.info(s"Processed '$name' in $elapsedTime msec")
  }

}
