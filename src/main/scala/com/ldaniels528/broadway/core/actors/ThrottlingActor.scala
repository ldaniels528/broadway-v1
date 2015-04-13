package com.ldaniels528.broadway.core.actors

import akka.actor.ActorRef

import scala.concurrent.duration._
import scala.language.postfixOps

/**
 * Throttling Actor
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
class ThrottlingActor(host: ActorRef, rateLimit: Double, enabled: Boolean = true) extends BroadwayActor {
  private val throttlePerMessage = Math.max(1000d / rateLimit, 1).toLong

  import context.dispatcher

  override def receive = {
    case message =>
      context.system.scheduler.scheduleOnce(throttlePerMessage.milliseconds, host, message)
      ()
  }

}
