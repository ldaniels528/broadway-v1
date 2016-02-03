package com.github.ldaniels528.broadway.core.actors

import akka.actor.ActorSystem

/**
  * Broadway Actor System
  * @author lawrence.daniels@gmail.com
  */
object BroadwayActorSystem {
  val system = ActorSystem("broadway_tasks")
  val scheduler = system.scheduler

  def shutdown() = system.shutdown()

}
