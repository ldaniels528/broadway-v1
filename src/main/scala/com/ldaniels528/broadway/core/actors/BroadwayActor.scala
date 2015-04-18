package com.ldaniels528.broadway.core.actors

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.dispatch.{BoundedMessageQueueSemantics, RequiresMessageQueue}

import scala.language.implicitConversions

/**
 * Abstract Broadway Actor
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
abstract class BroadwayActor() extends Actor
with RequiresMessageQueue[BoundedMessageQueueSemantics]
with ActorLogging {

}

/**
 * Broadway Actor Singleton
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
object BroadwayActor {

  object Implicits {

    implicit def actorOption(actor: ActorRef): Option[ActorRef] = Option(actor)

  }

}