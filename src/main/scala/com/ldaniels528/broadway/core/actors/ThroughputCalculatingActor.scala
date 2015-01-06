package com.ldaniels528.broadway.core.actors

import akka.actor.Actor
import com.ldaniels528.broadway.core.actors.Actors._
import com.ldaniels528.broadway.core.util.ThroughputCalculator

/**
 * Throughput Calculating Actor
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
class ThroughputCalculatingActor(host: BWxActorRef, listener: Double => Unit) extends Actor {
  private val calculator = new ThroughputCalculator(listener)

  override def receive = {
    case message =>
      host ! message
      calculator.update(1)
  }

}