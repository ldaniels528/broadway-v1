package com.ldaniels528.broadway.server

import akka.actor.ActorSystem
import com.ldaniels528.broadway.core.FileMonitor
import com.ldaniels528.broadway.core.Resources.FileResource
import com.ldaniels528.broadway.server.BroadwayServer._
import com.ldaniels528.broadway.server.datastore.DataStore
import com.ldaniels528.broadway.server.transporter.DataTransporter

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

    // watch the "incoming" directory for processing files
    fileWatcher.listenForFiles(config.getIncomingDirectory) { file =>
      transporter.process(file)
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
      case configPath :: Nil => ServerConfig.loadConfig(FileResource(configPath))
      case _ =>
        throw new IllegalArgumentException(s"${getClass.getName} [<config-file>]")
    }
    new BroadwayServer(config).start()
  }

}
