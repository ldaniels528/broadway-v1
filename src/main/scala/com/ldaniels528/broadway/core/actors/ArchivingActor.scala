package com.ldaniels528.broadway.core.actors

import java.io.File

import akka.actor.Actor
import com.ldaniels528.broadway.core.util.FileHelper
import com.ldaniels528.broadway.core.actors.ArchivingActor.ArchiveFile
import com.ldaniels528.broadway.server.ServerConfig

/**
 * This actor is responsible for archiving resources; moving them into a long-term storage area.
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
class ArchivingActor(config: ServerConfig) extends Actor {
  override def receive = {
    case file: File =>
      FileHelper.archive(file, config.getArchiveDirectory)
      ()
      
    case ArchiveFile(file, archiveDirectory) =>
      FileHelper.archive(file, archiveDirectory)
      ()

    case message =>
      unhandled(message)
  }
}

/**
 * Archival Actor Singleton
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
object ArchivingActor {

  case class ArchiveFile(file: File, archiveDirectory: File)

}