package com.github.ldaniels528.broadway.core.opcode.trigger

import java.io.File
import java.nio.file.StandardWatchEventKinds._
import java.nio.file.{Path, Paths, WatchService}

import com.github.ldaniels528.broadway.cli.actors.{ProcessingActor, TaskActorSystem}
import com.github.ldaniels528.broadway.core.ETLConfig
import org.slf4j.LoggerFactory

import scala.collection.JavaConversions._
import scala.collection.concurrent.TrieMap
import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

/**
  * File Trigger
  */
case class FileTrigger(id: String, path: String, feeds: Seq[FileFeed]) extends Trigger {

  override def execute(config: ETLConfig)(implicit ec: ExecutionContext) = {
    FileTrigger.register(watcherName = id, directory = new File(path)) { incomingFile =>
      feeds.find(_.matches(incomingFile)) foreach { feed =>
        logger.info(s"File triggered '${incomingFile.getAbsolutePath}' for '$id'...")

        ProcessingActor ! new Runnable {
          override def run() = {
            process(feed.flows, Seq(
              s"$id.path" -> incomingFile.getCanonicalPath,
              s"$id.filename" -> incomingFile.getName,
              s"$id.length" -> incomingFile.length()
            ))
          }
        }
      }
    }
  }

}

/**
  * File Trigger Companion Object
  */
object FileTrigger {
  private[this] val logger = LoggerFactory.getLogger(getClass)
  private[this] val registrations = TrieMap[File, Registration[_]]()
  private[this] val queue = TrieMap[File, QueuedFile[_]]()

  // continually poll for new files ...
  TaskActorSystem.system.scheduler.schedule(initialDelay = 0.seconds, interval = 1.seconds) {
    registrations foreach { case (directory, registration) =>
      checkForNewFiles(directory, registration)
    }
  }

  // continually check for files completion ...
  TaskActorSystem.system.scheduler.schedule(initialDelay = 0.seconds, interval = 5.seconds) {
    queue foreach { case (file, queuedFile) =>
      if (queuedFile.isReady) {
        queue.remove(file) foreach { qf =>
          logger.info(s"File '${file.getName}' is ready for processing...")
          // TODO an actor should perform the callback
          qf.registration.callback(file)
          ()
        }
      }
    }
  }

  def register[A](watcherName: String, directory: File)(callback: File => A) = {
    registrations.getOrElseUpdate(directory, {
      logger.info(s"$watcherName is watching for new files in '${directory.getAbsolutePath}'...")
      val path = Paths.get(directory.getAbsolutePath)
      val watcher = path.getFileSystem.newWatchService()
      val registration = Registration(watcherName, directory, path, watcher, callback)
      processPreExistingFiles(directory, registration)
      registration
    })
  }

  /**
    * Recursively schedules all files found in the given directory for processing
    *
    * @param directory    the given directory
    * @param registration the given registration
    */
  private def processPreExistingFiles[A](directory: File, registration: Registration[A]) {
    Option(directory.listFiles) foreach { files =>
      logger.info(s"Processing ${files.length} pre-existing files...")
      files foreach {
        case f if f.isFile => processFile(f, registration)
        case d if d.isDirectory => processPreExistingFiles(d, registration)
        case _ =>
      }
    }
  }

  /**
    * Schedules the given file for processing via the given callback function
    *
    * @param file         he given file
    * @param registration the given callback function
    */
  private def processFile[A](file: File, registration: Registration[A]) = notifyWhenReady(file, registration)

  /**
    * notifies the caller when the file is ready
    *
    * @param file         the given [[File]]
    * @param registration the given registration
    */
  private def notifyWhenReady[A](file: File, registration: Registration[A]) {
    queue.putIfAbsent(file, QueuedFile(file, registration))
    ()
  }

  private def checkForNewFiles[A](directory: File, registration: Registration[A]) = {
    Option(registration.watcher.poll()) foreach { watchKey =>
      val events = watchKey.pollEvents()
      if (events.nonEmpty) {
        for (event <- events) {
          if (event.kind() == ENTRY_CREATE || event.kind() == ENTRY_MODIFY) {
            // get a reference to the new file
            val file = new File(directory, event.context().toString)
            logger.info(s"Waiting to consume '${file.getName}' (${directory.getAbsolutePath})...")
            notifyWhenReady(file, registration)
          }
        }
      }
    }
  }

  /**
    * Represents a queued file
    */
  case class QueuedFile[A](file: File, registration: Registration[A]) {
    // capture the file's initial state
    private var state0 = FileChangeState(file.lastModified(), file.length())

    /**
      * Attempts to determine whether the file is complete or not
      *
      * @return true, if the file's size or last modified time hasn't changed in [up to] 10 seconds
      */
    def isReady: Boolean = {
      if (state0.elapsed < 1.second.toMillis) false
      else {
        // get the last modified time and file size
        val state1 = FileChangeState(file.lastModified(), file.length())

        // has the file changed?
        val unchanged = state0.time == state1.time && state0.size == state1.size
        if (!unchanged) {
          state0 = state1.copy(lastChange = System.currentTimeMillis())
        }

        // return the result
        state0.elapsed >= 30.seconds.toMillis && unchanged
      }
    }
  }

  case class FileChangeState(time: Long, size: Long, lastChange: Long = System.currentTimeMillis()) {
    def elapsed = System.currentTimeMillis() - lastChange
  }

  /**
    * Represents a file watching registration
    *
    * @param watcherName the given unique registration ID
    * @param directory   the directory to watch
    * @param path        the path to watch
    * @param watcher     the [[WatchService watch service]]
    * @param callback    the callback
    * @tparam A the callback return type
    */
  case class Registration[A](watcherName: String, directory: File, path: Path, watcher: WatchService, callback: File => A) {
    Seq(ENTRY_CREATE, ENTRY_MODIFY) foreach (path.register(watcher, _))
  }

}
