package com.ldaniels528.broadway.server

import akka.actor.ActorSystem
import com.ldaniels528.broadway.core.resources._
import com.ldaniels528.broadway.core.topology.TopologyConfig
import com.ldaniels528.broadway.server.BroadwayServer._
import com.ldaniels528.broadway.server.datastore.DataStore
import com.ldaniels528.broadway.server.transporter.{DataTransporter, FileMonitor}
import com.ldaniels528.trifecta.util.OptionHelper._

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
    val topologyConfigs = TopologyConfig.loadTopologyConfigs(config.getTopologiesDirectory)

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
