package com.ldaniels528.broadway.server

import java.io.{FilenameFilter, File}

import akka.actor.ActorSystem
import com.ldaniels528.broadway.core.FileMonitor
import com.ldaniels528.broadway.core.Resources.FileResource
import com.ldaniels528.broadway.core.topology.{TopologyConfig, TopologyConfigParser}
import com.ldaniels528.broadway.server.BroadwayServer._
import com.ldaniels528.broadway.server.datastore.DataStore
import com.ldaniels528.broadway.server.transporter.DataTransporter
import com.ldaniels528.trifecta.util.OptionHelper._
import org.slf4j.LoggerFactory

/**
 * Broadway Server
 * @param config the given [[ServerConfig]]
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
class BroadwayServer(config: ServerConfig) {
  private val system = ActorSystem("BroadwaySystem")
  private implicit val ec = system.dispatcher
  private val fileWatcher = new FileMonitor(system)
  private val dataStore = new DataStore(config)
  private val transporter = new DataTransporter(config)

  /**
   * Start the server
   */
  def start() {
    System.out.println(s"Broadway Server v$Version")

    // initialize the configuration
    config.init()

    // load the topology configurations
    val topologyConfigs = loadTopologyConfigs(config.getTopologiesDirectory)

    // setup listeners for all configured locations
    topologyConfigs foreach { tc =>
      tc.locations foreach { location =>
        location.toFile foreach { directory =>
          // watch the "incoming" directory for processing files
          fileWatcher.listenForFiles(directory) { file =>
            transporter.process(location, file)
          }
        }
      }
    }

    // watch the "completed" directory for archiving files
    fileWatcher.listenForFiles(config.getCompletedDirectory) { file =>
      dataStore.archive(file)
      ()
    }
    ()
  }

}

/**
 * Broadway Server Application
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
object BroadwayServer {
  private lazy val logger = LoggerFactory.getLogger(getClass)
  private val Version = "0.1"

  /**
   * Loads all topology configurations from the given directory
   * @param directory the given directory
   * @return the collection of successfully parsed [[TopologyConfig]] objects
   */
  def loadTopologyConfigs(directory: File): Seq[TopologyConfig] = {
    logger.info(s"Searching for topology configuration files in '${directory.getAbsolutePath}'...")
    val xmlFile = directory.listFiles(new FilenameFilter {
      override def accept(dir: File, name: String): Boolean = name.toLowerCase.endsWith(".xml")
    })
    xmlFile.toSeq flatMap (f => TopologyConfigParser.parse(FileResource(f.getAbsolutePath)))
  }

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
