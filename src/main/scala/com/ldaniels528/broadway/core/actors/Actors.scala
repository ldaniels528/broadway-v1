package com.ldaniels528.broadway.core.actors

import akka.actor.ActorRef
import com.ldaniels528.broadway.core.actors.FileReadingActor.TextFormatHandler

import scala.language.implicitConversions

/**
 * Broadway Actors
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
object Actors {

  type BWxActorRef = () => ActorRef

  object Implicits {

    implicit class ActorExtensions[T <: BWxActorRef](val actor: T) extends AnyVal {

      def !(message: Any): Unit = actor() ! message
    }

  }

}
