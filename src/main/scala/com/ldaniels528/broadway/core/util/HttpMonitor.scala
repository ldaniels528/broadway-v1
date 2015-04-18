package com.ldaniels528.broadway.core.util

import java.net.{HttpURLConnection, URL}

import akka.actor.ActorSystem
import org.slf4j.LoggerFactory

import scala.collection.concurrent.TrieMap
import scala.concurrent.duration._
import scala.util.Try

/**
 * HTTP Monitor
 * @param system the given [[ActorSystem]]
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
class HttpMonitor(system: ActorSystem) {
  private lazy val logger = LoggerFactory.getLogger(getClass)
  private implicit val ec = system.dispatcher
  private val sites = TrieMap[URL, URL => Unit]()
  private val found = TrieMap[URL, Boolean]()

  // check for new resources every 5 minutes
  system.scheduler.schedule(0.seconds, 5.minutes, new Runnable {
    override def run() = checkSitesForFiles()
  })

  def listenForResources(watcherName:String, urls: Seq[String])(callback: URL => Unit): Unit = {
    sites ++= urls.map { path =>
      logger.info(s"$watcherName is watching for updates to resource '$path'...")
      new URL(path) -> callback
    }
    ()
  }

  private def checkSitesForFiles(): Unit = {
    sites foreach { case (url, callback) =>
      if(!found.contains(url)) {
        Try(url.openConnection()).foreach {
          case conn: HttpURLConnection =>
            found += url -> true
            callback(url)
          case unknown =>
            logger.error(s"Unknown connection type $unknown (${Option(unknown).map(_.getClass.getName).orNull})")
        }
      }
    }
  }

}
