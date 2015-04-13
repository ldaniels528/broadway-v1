package com.ldaniels528.broadway.core.actors.file

import java.io.File

import akka.actor.{Actor, ActorLogging}
import com.ldaniels528.broadway.core.actors.BroadwayActor
import com.ldaniels528.broadway.core.actors.file.ArchivingActor.ArchiveFile
import com.ldaniels528.broadway.core.resources.{FileResource, ReadableResource}
import com.ldaniels528.broadway.core.util.FileHelper
import com.ldaniels528.broadway.server.ServerConfig

/**
 * This actor is responsible for archiving resources; moving them into a long-term storage area.
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
class ArchivingActor(config: ServerConfig) extends BroadwayActor {
  override def receive = {
    case ArchiveFile(file, archiveDirectory) =>
      FileHelper.archive(file, archiveDirectory)
      ()

    case file: File =>
      FileHelper.archive(file, config.getArchiveDirectory)
      ()

    case resource: ReadableResource =>
      resource match {
        case FileResource(path) =>
          FileHelper.archive(new File(path), config.getArchiveDirectory)
          ()
        case _ =>
          log.warning(s"Resource '$resource' cannot be moved to archive")
      }

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