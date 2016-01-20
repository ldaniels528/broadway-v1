package com.github.ldaniels528.broadway.cli.actors

import akka.actor.ActorSystem

/**
  * Created by ldaniels on 1/18/16.
  */
object TaskActorSystem {
  val system = ActorSystem("metis_tasks")

  def shutdown() = system.shutdown()

}
