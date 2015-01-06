package com.ldaniels528.broadway.core.actors

import akka.actor.ActorRef
import akka.pattern._

import scala.concurrent.{ExecutionContext, Future}
import scala.language.implicitConversions

/**
 * Broadway Actors
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
object Actors {

  /**
   * Defines the function to an actor reference used through Broadway's API
   */
  type BWxActorRef = () => ActorRef

  /**
   * Extensions to support tell and ask functions
   * @param actor the given [[BWxActorRef]]
   * @tparam T represents [[BWxActorRef]] types
   */
  implicit class ActorExtensions[T <: BWxActorRef](val actor: T) extends AnyVal {

    def !(message: Any): Unit = actor() ! message

    def ?(message: Any)(implicit timeout: akka.util.Timeout): Future[Any] = actor() ? message

    def <~[S](message: S)(implicit ec: ExecutionContext, timeout: akka.util.Timeout): Future[S] = {
      (actor() ? message).map(_.asInstanceOf[S])
    }

  }

}
