package com.ldaniels528.broadway.core.actors

import akka.actor.{Actor, ActorLogging}
import akka.dispatch.{BoundedMessageQueueSemantics, RequiresMessageQueue}

/**
 * Abstract Broadway Actor
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
abstract class BroadwayActor() extends Actor
with RequiresMessageQueue[BoundedMessageQueueSemantics]
with ActorLogging {

}
