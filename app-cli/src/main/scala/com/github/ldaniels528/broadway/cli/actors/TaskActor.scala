package com.github.ldaniels528.broadway.cli.actors

import akka.actor.{Actor, ActorLogging}
import com.github.ldaniels528.broadway.cli.actors.TaskActor.{Dead, Die}

/**
  * Task Actor
  */
class TaskActor() extends Actor with ActorLogging {

  override def receive = {
    case task: Runnable => task.run()
    case Die =>
      sender ! Dead
      context.stop(self)
    case message =>
      log.warning(s"Unhandled message '$message' (${Option(message).map(_.getClass.getName).orNull})")
      unhandled(message)
  }

}

/**
  * Task Actor Companion Object
  */
object TaskActor {

  case object Die

  case object Dead

}