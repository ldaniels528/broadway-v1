package com.ldaniels528.broadway.server

import java.io.File
import java.net.URL

import akka.actor.Actor
import com.ldaniels528.broadway.BroadwayNarrative
import com.ldaniels528.broadway.core.location.{FileLocation, HttpLocation, Location}
import com.ldaniels528.broadway.core.narrative._
import com.ldaniels528.broadway.core.resources._
import com.ldaniels528.broadway.core.util.FileHelper._
import com.ldaniels528.broadway.core.util.{FileMonitor, HttpMonitor}
import com.ldaniels528.broadway.server.BroadwayServer._
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

  // create the system actors
  private lazy val archivingActor = config.archivingActor
  private lazy val processingActor = config.addActor(new TopologyProcessingActor(config))

  // setup the HTTP server
  private val httpServer = config.httpInfo.map(hi => new BroadwayHttpServer(host = hi.host, port = hi.port))

  /**
   * Start the server
   */
  def start() {
    logger.info(s"Broadway Server v$Version")

    // initialize the configuration
    config.init()

    // optionally start the HTTP server
    for {
      info <- config.httpInfo
      listener <- httpServer
    } {
      logger.info(s"Starting HTTP listener (interface ${info.host} port ${info.port})...")
      listener.start()
    }

    // load the topology configurations
    val topologyConfigs = NarrativeConfig.loadNarrativeConfigs(config.getTopologiesDirectory)

    // setup listeners for all configured locations and triggers
    topologyConfigs foreach { tc =>

      // setup scheduled jobs
      system.scheduler.schedule(0.seconds, 5.minute, new Runnable {
        override def run() {
          tc.triggers foreach { trigger =>
            if (trigger.isReady(System.currentTimeMillis())) {
              rt.getNarrative(config, trigger.narrative) foreach { narrative =>
                logger.info(s"Invoking narrative '${trigger.narrative.id}'...")
                val resource = trigger.resource.getOrElse(LoopbackResource(s"T${System.currentTimeMillis}"))
                processingActor ! RunJob(narrative, resource)
              }
            }
          }
        }
      })

      // watch the "incoming" directories for processing files
      tc.locations foreach { location =>
        logger.info(s"Configuring location ${location.id}...")
        location match {
          case site@FileLocation(id, path, feeds) =>
            fileMonitor.listenForFiles(directory = new File(path))(handleIncomingFile(site, _))

          // watch for HTTP files
          case site@HttpLocation(id, path, feeds) =>
            val urls = feeds.map(f => s"${site.path}${f.name}")
            httpMonitor.listenForResources(urls)(handleIncomingResource(site, _))

          case site =>
            logger.warn(s"Listening is not supported by location '${site.id}'")
        }
      }
    }

    // watch the "completed" directory for archiving files
    fileMonitor.listenForFiles(config.getCompletedDirectory)(archivingActor ! _)
    ()
  }

  /**
   * Handles the the given incoming file
   * @param directory the given [[FileLocation]]
   * @param file the given incoming [[File]]
   */
  private def handleIncomingFile(directory: Location, file: File) {
    directory.findFeed(file.getName) match {
      case Some(feed) => processFeed(feed, file)
      case None => noMappedProcess(directory, file)
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
   * Processes the given feed via a topology
   * @param feed the given [[Feed]]
   * @param file the given [[File]]
   */
  private def processFeed(feed: Feed, file: File) = {
    feed.topology foreach { td =>
      // lookup the topology
      rt.getNarrative(config, td) match {
        case Success(topology) =>
          val fileName = file.getName
          logger.info(s"${topology.name}: Moving file '$fileName' to '${config.getWorkDirectory}' for processing...")
          val wipFile = new File(config.getWorkDirectory, fileName)
          move(file, wipFile)

          // start the topology using the file as its input source
          processingActor ! RunJob(topology, FileResource(wipFile.getAbsolutePath))

        case Failure(e) =>
          if (!reported.contains(td.id)) {
            logger.error(s"${td.id}: Topology could not be instantiated", e)
            reported += td.id -> e
          }
      }

    }
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
  private val Version = "0.7"

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

  /**
   * This is an internal use actor that is responsible for processing topologies
   * @author Lawrence Daniels <lawrence.daniels@gmail.com>
   */
  class TopologyProcessingActor(config: ServerConfig) extends Actor {
    override def receive = {
      case RunJob(narrative, resource) =>
        narrative.start(resource)
      case message =>
        unhandled(message)
    }
  }

  /**
   * This message causes the the given narrative to be invoked; consuming the given resource
   * @param narrative the given [[BroadwayNarrative]]
   * @param resource the given [[Resource]]
   */
  case class RunJob(narrative: BroadwayNarrative, resource: Resource)

}
