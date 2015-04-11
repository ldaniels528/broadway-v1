package com.ldaniels528.broadway.server.http

import akka.actor._
import akka.io.IO
import akka.io.Tcp.Bound
import akka.pattern.ask
import akka.util.Timeout
import org.slf4j.LoggerFactory
import spray.can.Http

import scala.concurrent.duration._
import scala.language.postfixOps

/**
 * Broadway Http Server
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
class BroadwayHttpServer(bsc: ServerContext, host: String, port: Int)(implicit system: ActorSystem) {
  private lazy val logger = LoggerFactory.getLogger(getClass)
  private val listenerActor = system.actorOf(Props(new ClientHandlingActor(bsc)), "clientHandler")

  import system.dispatcher

  def start(): Unit = {
    // start the HTTP server
    implicit val timeout: Timeout = 5 seconds
    val response = IO(Http) ? Http.Bind(listenerActor, interface = host, port = port)
    response.foreach {
      case Bound(interface) =>
        logger.info(s"Server is now bound to $interface")
      case outcome =>
        logger.error(s"Unexpected response $outcome")
    }
  }

}
