package com.ldaniels528.broadway.core.actors

import java.util

import akka.actor.Actor
import com.ldaniels528.broadway.core.actors.Actors.BWxActorRef
import com.ldaniels528.broadway.core.actors.Actors.Implicits._
import com.ldaniels528.broadway.server.ServerConfig
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Success, Try}

/**
 * Throttling Actor
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
class ThrottlingActor(config: ServerConfig, actor: BWxActorRef, rateLimit: Double)(implicit ec: ExecutionContext) extends Actor {
  private lazy val logger = LoggerFactory.getLogger(getClass)
  private val queue = new util.Stack[Any]()
  private var lastUpdate = System.currentTimeMillis()
  private var lastMessageCount = 0
  private var messageCount = 0
  private var rate: Double = 0

  override def receive = {
    case message =>
      queue.push(message)
      if (computeRate() < rateLimit) sendNextMessage() else scheduleNextMessage()
  }

  private def scheduleNextMessage(): Unit = {
    logger.info(f"rate = $rate%.1f")
    config.system.scheduler.scheduleOnce(1000 millis) {
      if (computeRate() < rateLimit) {
        sendNextMessage()
      }
      else scheduleNextMessage()
    }
  }

  private def sendNextMessage() {
      Try(queue.pop()) match {
        case Success(message) =>
          actor ! message
          messageCount += 1
        case _ =>
    }
  }

  private def computeRate(): Double = {
    val deltaTime = (System.currentTimeMillis() - lastUpdate).toDouble / 1000d
    if (deltaTime >= 1d) {
      val count = messageCount - lastMessageCount
      rate = count.toDouble / deltaTime
      lastMessageCount = messageCount
      lastUpdate = System.currentTimeMillis()
    }
    rate
  }

}
