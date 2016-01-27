package com.github.ldaniels528.broadway.core.actors

import akka.actor.ActorSystem

/**
  * Created by ldaniels on 1/18/16.
  */
object BroadwayActorSystem {
  val system = ActorSystem("broadway_tasks")

  def shutdown() = system.shutdown()

}
