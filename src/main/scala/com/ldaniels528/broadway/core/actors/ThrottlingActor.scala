package com.ldaniels528.broadway.core.actors

import akka.actor.Actor
import com.ldaniels528.broadway.core.actors.Actors.BWxActorRef
import com.ldaniels528.broadway.core.actors.Actors.Implicits._
import com.ldaniels528.broadway.server.ServerConfig
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext
import scala.language.postfixOps

/**
 * Throttling Actor
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
class ThrottlingActor(config: ServerConfig, actor: BWxActorRef, rateLimit: Double)(implicit ec: ExecutionContext) extends Actor {
  private lazy val logger = LoggerFactory.getLogger(getClass)
  private val throttlePerMessage = Math.max(1000d / rateLimit, 1).toLong
  private var lastUpdate = System.currentTimeMillis()
  private var lastLogUpdate = System.currentTimeMillis()
  private var lastMessageCount = 0
  private var messageCount = 0
  private var rate: Double = 0

  override def receive = {
    case message =>
      actor ! message
      messageCount += 1
      computeRate()
      Thread.sleep(throttlePerMessage)
  }

  private def computeRate(): Double = {
    val deltaTime = (System.currentTimeMillis() - lastUpdate).toDouble / 1000d
    if (deltaTime >= 1d) {
      val count = messageCount - lastMessageCount
      rate = count.toDouble / deltaTime
      lastMessageCount = messageCount
      lastUpdate = System.currentTimeMillis()

      if(System.currentTimeMillis() - lastLogUpdate >= 15000) {
        logger.info(f"Throughput rate is $rate%.1f")
        lastLogUpdate = System.currentTimeMillis()
      }
    }
    rate
  }

}
