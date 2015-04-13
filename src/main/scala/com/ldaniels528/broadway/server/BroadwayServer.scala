package com.ldaniels528.broadway.server

import java.io.File
import java.net.URL
import java.util.concurrent.atomic.AtomicLong

import akka.actor.Props
import akka.routing.RoundRobinPool
import com.ldaniels528.broadway.core.actors.NarrativeProcessingActor
import com.ldaniels528.broadway.core.actors.NarrativeProcessingActor.RunJob
import com.ldaniels528.broadway.core.triggers.location.{FileLocation, HttpLocation, Location}
import com.ldaniels528.broadway.core.narrative._
import com.ldaniels528.broadway.core.resources._
import com.ldaniels528.broadway.core.util.FileHelper._
import com.ldaniels528.broadway.core.util.{FileMonitor, HttpMonitor}
import com.ldaniels528.broadway.server.BroadwayServer._
import com.ldaniels528.broadway.server.http.BroadwayHttpServer
import com.ldaniels528.trifecta.util.OptionHelper._
import org.slf4j.LoggerFactory

import scala.collection.concurrent.TrieMap
import scala.concurrent.duration._
import scala.util.{Failure, Success}

/**
 * Broadway Server
 * @param config the given [[ServerConfig]]
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
class BroadwayServer(config: ServerConfig) {
  private lazy val logger = LoggerFactory.getLogger(getClass)
  private implicit val system = config.system
  private implicit val ec = system.dispatcher
  private implicit val rt = new NarrativeRuntime()
  private val fileMonitor = new FileMonitor(system)
  private val httpMonitor = new HttpMonitor(system)
  private val reported = TrieMap[String, Throwable]()
  private val counter = new AtomicLong(System.currentTimeMillis())

  import config.archivingActor

  // create the narrative processing actor
  private val processingActor = system.actorOf(Props(new NarrativeProcessingActor(config)).withRouter(RoundRobinPool(nrOfInstances = 10)))

  /**
   * Start the server
   */
  def start() {
    logger.info(s"Broadway Server v$Version")

    // initialize the configuration
    val bsc = config.init()

    // setup the HTTP server
    val httpServer = config.httpInfo.map(hi => new BroadwayHttpServer(bsc, host = hi.host, port = hi.port))

    // optionally start the HTTP server
    for {
      info <- config.httpInfo
      listener <- httpServer
    } {
      logger.info(s"Starting HTTP listener (interface ${info.host} port ${info.port})...")
      listener.start()
    }

    // load the anthologies
    bsc.anthologies foreach { anthology =>
      logger.info(s"Configuring anthology '${anthology.id}'...")

      // setup scheduled jobs
      system.scheduler.schedule(0.seconds, 5.minute, new Runnable {
        override def run() {
          anthology.triggers foreach { trigger =>
            if (trigger.isReady(System.currentTimeMillis())) {
              rt.getNarrative(config, trigger.narrative) foreach { narrative =>
                logger.info(s"Invoking narrative '${trigger.narrative.id}'...")
                processingActor ! RunJob(narrative, trigger.resource)
              }
            }
          }
        }
      })

      // watch the "incoming" directories for processing files
      anthology.locations foreach { location =>
        logger.info(s"Configuring location '${location.id}'...")
        location match {
          case site@FileLocation(id, path, feeds) =>
            fileMonitor.listenForFiles(id, directory = new File(path))(handleIncomingFile(site, _))

          // watch for HTTP files
          case site@HttpLocation(id, path, feeds) =>
            val urls = feeds.map(f => s"${site.path}${f.name}")
            httpMonitor.listenForResources(id, urls)(handleIncomingResource(site, _))

          case site =>
            logger.warn(s"Listening is not supported by location '${site.id}'")
        }
      }
    }

    // watch the "completed" directory for archiving files
    fileMonitor.listenForFiles("Broadway", config.getCompletedDirectory)(archivingActor ! _)
    ()
  }

  /**
   * Handles the the given incoming file
   * @param location the given [[FileLocation]]
   * @param file the given incoming [[File]]
   */
  private def handleIncomingFile(location: Location, file: File) {
    location.findFeed(file.getName) match {
      case Some(feed) => processFeed(feed, file)
      case None => noMappedProcess(location, file)
    }
    ()
  }

  /**
   * Handles the the given incoming URL
   * @param site the given [[HttpLocation]]
   * @param url the given incoming [[URL]]
   */
  private def handleIncomingResource(site: HttpLocation, url: URL) {
    logger.info(s"url: $url")
  }

  /**
   * Processes the given feed via a narrative
   * @param feed the given [[Feed]]
   * @param file the given [[File]]
   */
  private def processFeed(feed: Feed, file: File) = {
    // TODO what about feeds that have no narrative?
    feed.descriptor foreach { descriptor =>
      // lookup the narrative
      rt.getNarrative(config, descriptor) match {
        case Success(narrative) =>
          val fileName = file.getName
          val workDir = createWorkDirectory()

          logger.info(s"${narrative.name}: Moving file '$fileName' to '${workDir.getAbsolutePath}' for processing...")
          val wipFile = new File(workDir, fileName)
          if (move(file, wipFile)) {
            // start the narrative using the file as its input source
            processingActor ! RunJob(narrative, Some(FileResource(wipFile.getAbsolutePath)))
          }
          else {
            logger.error(s"Failed to move the work file ${wipFile.getName} into ${workDir.getAbsolutePath}")
          }

        case Failure(e) =>
          if (!reported.contains(descriptor.id)) {
            logger.error(s"${descriptor.id}: Narrative could not be instantiated", e)
            reported += descriptor.id -> e
          }
      }
    }
  }

  private def createWorkDirectory(): File = {
    val workDir = new File(config.getWorkDirectory, String.valueOf(counter.incrementAndGet()))
    if (!workDir.exists() && !workDir.mkdirs()) {
      logger.warn(s"Failed to create the work directory: ${workDir.getAbsolutePath}")
    }
    workDir
  }

  /**
   * Called when no mapping process is found for the given file
   * @param file the given [[File]]
   */
  private def noMappedProcess(location: Location, file: File) = {
    val fileName = file.getName
    logger.info(s"${location.id}: No mappings found for '$fileName'. Moving to '${config.getCompletedDirectory}' for archival.")
    move(file, new File(config.getCompletedDirectory, fileName))
  }

}

/**
 * Broadway Server Application
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
object BroadwayServer {
  private val Version = "0.9.0"

  /**
   * Enables command line execution
   * {{{ broadway.sh /usr/local/java/broadway/server-config.properties }}}
   * @param args the given command line arguments
   */
  def main(args: Array[String]) {
    // load the configuration
    val config = args.toList match {
      case Nil => ServerConfig()
      case configPath :: Nil => ServerConfig(FileResource(configPath))
      case _ =>
        throw new IllegalArgumentException(s"${getClass.getName} [<config-file>]")
    }
    new BroadwayServer(config.orDie("No configuration file (broadway-config.xml) found")).start()
  }

}
