package com.ldaniels528.broadway.core.actors

import akka.actor.Actor
import com.ldaniels528.broadway.core.actors.Actors.BWxActorRef
import com.ldaniels528.broadway.core.actors.Actors.Implicits._

import scala.concurrent.ExecutionContext
import scala.language.postfixOps

/**
 * Throttling Actor
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
class ThrottlingActor(actor: BWxActorRef, rateLimit: Double)(implicit ec: ExecutionContext) extends Actor {
  private val throttlePerMessage = Math.max(1000d / rateLimit, 1).toLong

  override def receive = {
    case message =>
      Thread.sleep(throttlePerMessage)
      actor ! message
  }

}
