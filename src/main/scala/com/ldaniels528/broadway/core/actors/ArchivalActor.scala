package com.ldaniels528.broadway.core.actors

import java.io.File

import akka.actor.Actor
import com.ldaniels528.broadway.core.util.FileHelper
import com.ldaniels528.broadway.core.actors.ArchivalActor.Archive

/**
 * This actor is responsible for archiving resources; moving them into a long-term storage area.
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
class ArchivalActor() extends Actor {
  override def receive = {
    case Archive(file, archiveDirectory) =>
      FileHelper.archive(file, archiveDirectory)

    case message =>
      unhandled(message)
  }
}

/**
 * Archival Actor Singleton
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
object ArchivalActor {

  case class Archive(file: File, archiveDirectory: File)

}