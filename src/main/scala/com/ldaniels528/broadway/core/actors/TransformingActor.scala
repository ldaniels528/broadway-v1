package com.ldaniels528.broadway.core.actors

import akka.actor.Actor

/**
 * Data Transforming Actor
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
class TransformingActor(transform: Any => Boolean) extends Actor {
  override def receive = {
    case message =>
      if(!transform(message)) unhandled(message)
  }
}
