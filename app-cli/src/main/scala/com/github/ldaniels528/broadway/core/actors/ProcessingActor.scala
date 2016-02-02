package com.github.ldaniels528.broadway.core.actors

import akka.actor.{Actor, ActorLogging, Props}
import akka.routing.RoundRobinPool

/**
  * Processing Actor
  * @author lawrence.daniels@gmail.com
  */
class ProcessingActor() extends Actor with ActorLogging {

  override def receive = {
    case task: Runnable =>
      task.run()

    case message =>
      log.warning(s"Unhandled message '$message' (${Option(message).map(_.getClass.getName).orNull})")
      unhandled(message)
  }

}

/**
  * Processing Actor Companion Object
  * @author lawrence.daniels@gmail.com
  */
object ProcessingActor {
  private val actors = BroadwayActorSystem.system.actorOf(Props[ProcessingActor].withRouter(RoundRobinPool(nrOfInstances = 1)))

  def !(task: Runnable) = actors ! task

}