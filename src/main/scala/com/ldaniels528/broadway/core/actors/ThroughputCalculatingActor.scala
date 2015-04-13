package com.ldaniels528.broadway.core.actors

import akka.actor.ActorRef
import com.ldaniels528.broadway.core.util.ThroughputCalculator

/**
 * Throughput Calculating Actor
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
class ThroughputCalculatingActor(host: ActorRef, listener: Double => Unit) extends BroadwayActor {
  private val calculator = new ThroughputCalculator(listener)

  override def receive = {
    case message =>
      host ! message
      calculator.update(1)
  }

}