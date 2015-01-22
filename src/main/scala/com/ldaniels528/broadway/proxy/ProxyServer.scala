package com.ldaniels528.broadway.proxy

import java.io.{BufferedInputStream, BufferedOutputStream}
import java.net.{ServerSocket, Socket}

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import org.slf4j.LoggerFactory

import scala.concurrent.Future
import scala.util.Try

/**
 * Proxy Server
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
object ProxyServer {
  private val logger = LoggerFactory.getLogger(getClass)
  private val system = ActorSystem(name = "ProxySystem")
  private implicit val ec = system.dispatcher
  private val actors = (1 to 10) map (n => system.actorOf(Props[ProxyActor]()))
  private var alive = true
  private var ticker = 0

  def main(args: Array[String]): Unit = {
    // check the command line arguments
    if (args.length < 3 || args.length % 3 != 0) {
      throw new IllegalArgumentException(s"Usage: ${getClass.getName} <srcPrt> <dstHost> <dstPort>")
    }

    val endPoints = args.toList.sliding(3, 3) map {
      case aSrcPort :: aDestHost :: aDestPort :: Nil => EndPoint(aSrcPort.toInt, aDestHost, aDestPort.toInt)
    }

    execute(endPoints)
  }

  def actor: ActorRef = {
    ticker += 1
    actors(ticker % actors.length)
  }

  def die() = alive = false

  /**
   * Runs the proxy
   */
  def execute(endPoints: Iterator[EndPoint]) {
    System.out.println("ProxyServer v0.01")

    endPoints foreach { case EndPoint(srcPort, destHost, destPort) =>

      // listen on the source port
      val inputSocket = new ServerSocket(srcPort)
      val outputSocket = new Socket(destHost, destPort)

      logger.info(s"Listening for connections: $srcPort ~> $destHost:$destPort")
      Future {
        while (alive) {
          actor ! Pipe(srcPort, destPort, inputSocket.accept(), outputSocket)
        }
      }
    }
  }

  private def handlePipe(srcPort: Int, destPort: Int, peerA: Socket, peerB: Socket) {
    // get the host names for each peer
    val hostA = s"${peerA.getInetAddress.getHostName}:$srcPort"
    val hostB = s"${peerA.getInetAddress.getHostName}:$destPort"

    // generate labels for hosts A & B
    val labelA = if (hostA != hostB) hostA else s"A:$srcPort"
    val labelB = if (hostA != hostB) hostB else s"B:$destPort"
    logger.info(s"$labelA is now connected to $labelB")

    // stream the data from peer A to peer B (and vice versa)
    stream(peerA, peerB, labelA, labelB)
    stream(peerB, peerA, labelB, labelA)
  }

  private def stream(peerA: Socket, peerB: Socket, labelA: String, labelB: String) {
    Future {
      // get the input for Peer A and the output for Peer B
      val in = new BufferedInputStream(peerA.getInputStream, 66536)
      val out = new BufferedOutputStream(peerB.getOutputStream, 66536)

      // stream the input of peer A to the output of peer B
      val buf = new Array[Byte](65536)
      while (peerA.isConnected && peerB.isConnected) {
        val count = in.read(buf)
        if (count > 0) {
          logger.info(s"Copying $count bytes: $labelA ~> $labelB")
          out.write(buf, 0, count)
          out.flush()
        }
      }

      logger.info(s"$labelA is ${if (peerA.isConnected) "Connected" else "Disconnected"}")
      logger.info(s"$labelB is ${if (peerB.isConnected) "Connected" else "Disconnected"}")
      Try(peerA.close())
      Try(peerB.close())
    }
  }

  /**
   * Proxy Actor
   * @author Lawrence Daniels <lawrence.daniels@gmail.com>
   */
  class ProxyActor() extends Actor {
    override def receive = {
      case Pipe(srcPort, destPort, inSocket, outSocket) => handlePipe(srcPort, destPort, inSocket, outSocket)
      case message => unhandled(message)
    }
  }

  case class Pipe(srcPort: Int, destPort: Int, inSocket: Socket, outSocket: Socket)

  case class EndPoint(srcPort: Int, destHost: String, destPort: Int)

}
