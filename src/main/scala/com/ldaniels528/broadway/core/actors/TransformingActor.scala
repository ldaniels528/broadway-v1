package com.ldaniels528.broadway.core.actors

/**
 * Data Transforming Actor
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
class TransformingActor(transform: Any => Boolean) extends BroadwayActor {
  override def receive = {
    case message =>
      if (!transform(message)) unhandled(message)
  }
}
