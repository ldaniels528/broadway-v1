package com.ldaniels528.broadway.core.actors

import akka.actor.Actor
import com.ldaniels528.broadway.core.actors.Actors.BWxActorRef
import com.ldaniels528.broadway.core.actors.Actors.Implicits._

import scala.language.postfixOps

/**
 * Throttling Actor
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
class ThrottlingActor(host: BWxActorRef, rateLimit: Double, enabled: Boolean = true) extends Actor {
  private val throttlePerMessage = Math.max(1000d / rateLimit, 1).toLong

  override def receive = {
    case message =>
      if(enabled) Thread.sleep(throttlePerMessage)
      host ! message
  }

}
