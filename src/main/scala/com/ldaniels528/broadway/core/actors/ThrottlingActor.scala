package com.ldaniels528.broadway.core.actors

import akka.actor.{Actor, ActorRef}

import scala.language.postfixOps

/**
 * Throttling Actor
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
class ThrottlingActor(host: ActorRef, rateLimit: Double, enabled: Boolean = true) extends Actor {
  private val throttlePerMessage = Math.max(1000d / rateLimit, 1).toLong

  override def receive = {
    case message =>
      if (enabled) Thread.sleep(throttlePerMessage)
      host ! message
  }

}
