package com.ldaniels528.broadway.core.actors

import akka.actor.ActorRef
import akka.pattern._

import scala.concurrent.Future
import scala.language.implicitConversions

/**
 * Broadway Actors
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
object Actors {

  type BWxActorRef = () => ActorRef

  /**
   * Actor implicits
   */
  object Implicits {

    implicit class ActorExtensions[T <: BWxActorRef](val actor: T) extends AnyVal {

      def !(message: Any): Unit = actor() ! message

      def ?(message: Any)(implicit timeout: akka.util.Timeout): Future[Any] = actor() ? message

    }
  }

}
