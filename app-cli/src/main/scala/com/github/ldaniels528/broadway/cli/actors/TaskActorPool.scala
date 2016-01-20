package com.github.ldaniels528.broadway.cli.actors

import akka.actor.Props
import akka.pattern.ask
import akka.routing.RoundRobinPool
import akka.util.Timeout
import com.github.ldaniels528.broadway.cli.actors.TaskActor.Die

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

/**
  * Task Actor Pool
  */
class TaskActorPool(concurrency: Int) {
  private val taskActor = TaskActorSystem.system.actorOf(Props[TaskActor].withRouter(RoundRobinPool(nrOfInstances = concurrency)))

  def !(message: Any) = taskActor ! message

  def die(maxWait: FiniteDuration)(implicit ec: ExecutionContext) = {
    implicit val timeout: Timeout = maxWait
    Future.sequence(for (_ <- 1 to concurrency) yield taskActor ? Die)
  }

}
