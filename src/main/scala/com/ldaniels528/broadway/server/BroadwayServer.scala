package com.ldaniels528.broadway.server

import akka.actor.ActorSystem
import com.ldaniels528.broadway.core.FileMonitor
import com.ldaniels528.broadway.server.datastore.DataStore
import com.ldaniels528.broadway.server.transporter.DataTransporter

/**
 * Broadway Server Application
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
object BroadwayServer {
  private val Version = "0.1"
  private val config = ServerConfig.loadConfig()
  private val system = ActorSystem("BroadwaySystem")
  private implicit val ec = system.dispatcher
  private val fileWatcher = new FileMonitor(system)
  private val dataStore = new DataStore(config)
  private val transporter = new DataTransporter(config)

  /**
   * Enables command line execution
   * {{{ broadway.sh --etl nasdaqFileFlow.xml --input-source "/data/AMEX.txt" }}}
   * @param args the given command line arguments
   */
  def main(args: Array[String]): Unit = execute(args)

  /**
   * Executes the topology
   * @param args the given command line arguments
   */
  def execute(args: Array[String]) {
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
