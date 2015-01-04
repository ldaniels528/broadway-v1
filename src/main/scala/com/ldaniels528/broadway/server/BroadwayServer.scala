package com.ldaniels528.broadway.server

import java.io.File

import com.ldaniels528.broadway.core.actors.Actors.Implicits._
import com.ldaniels528.broadway.core.resources._
import com.ldaniels528.broadway.core.topology.{Feed, Location, TopologyConfig, TopologyRuntime}
import com.ldaniels528.broadway.core.util.FileHelper._
import com.ldaniels528.broadway.core.util.FileMonitor
import com.ldaniels528.broadway.server.BroadwayServer._
import com.ldaniels528.trifecta.util.OptionHelper._
import org.slf4j.LoggerFactory

import scala.collection.concurrent.TrieMap
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
  private implicit val rt = new TopologyRuntime()
  private val fileWatcher = new FileMonitor(system)
  private val reported = TrieMap[String, Throwable]()

  // create the system actors
  private val archivingActor = config.archivingActor

  /**
   * Start the server
   */
  def start() {
    System.out.println(s"Broadway Server v$Version")

    // initialize the configuration
    config.init()

    // load the topology configurations
    val topologyConfigs = TopologyConfig.loadTopologyConfigs(config.getTopologiesDirectory)

    // setup listeners for all configured locations
    topologyConfigs foreach { tc =>
      tc.locations foreach { location =>
        location.toFile foreach { directory =>
          // watch the "incoming" directory for processing files
          fileWatcher.listenForFiles(directory) { file =>
            handleIncomingFile(location, file)
          }
        }
      }
    }

    // watch the "completed" directory for archiving files
    fileWatcher.listenForFiles(config.getCompletedDirectory) { file =>
      archivingActor ! file
      ()
    }
    ()
  }

  /**
   * Handles the the given incoming file
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
   * Processes the given feed via a topology
   * @param feed the given [[Feed]]
   * @param file the given [[File]]
   */
  private def processFeed(feed: Feed, file: File) = {
    feed.topology foreach { td =>
      // lookup the topology
      rt.getTopology(config, td) match {
        case Success(topology) =>
          val fileName = file.getName
          logger.info(s"${topology.name}: Moving file '$fileName' to '${config.getWorkDirectory}' for processing...")
          val wipFile = new File(config.getWorkDirectory, fileName)
          move(file, wipFile)

          // start the topology using the file as its input source
          topology.start(FileResource(wipFile.getAbsolutePath))

        /*
        executeTopology(topology, wipFile) onComplete {
          case Success(result) =>
            move(wipFile, new File(config.getCompletedDirectory, fileName))
          case Failure(e) =>
            logger.error(s"${topology.name}: File '$fileName' failed during processing", e)
            move(wipFile, new File(config.getFailedDirectory, fileName))
        }*/
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
  private val Version = "0.1"

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
