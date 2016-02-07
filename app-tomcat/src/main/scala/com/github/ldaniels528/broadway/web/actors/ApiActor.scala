package com.github.ldaniels528.broadway.web.actors

import akka.actor.{Actor, ActorLogging}
import com.github.ldaniels528.broadway.web.routes.ApplicationRoutes
import spray.routing.{ExceptionHandler, Route}

/**
  * REST API Actor
  * @author lawrence.daniels@gmail.com
  */
class ApiActor extends Actor with ActorLogging with ApplicationRoutes {

  override implicit def actorRefFactory = context

  override def receive = runRoute(applicationRoutes)

  implicit val eh = new ExceptionHandler {

    override def isDefinedAt(cause: Throwable): Boolean = {
      log.error("Exception occurred", cause)
      true
    }

    override def apply(v1: Throwable): Route = ???
  }

}
