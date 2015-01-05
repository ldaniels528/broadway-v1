package com.ldaniels528.broadway.core.util

import org.slf4j.LoggerFactory

/**
 * Throughput Calculator
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
class ThroughputCalculator(label: String) {
  private lazy val logger = LoggerFactory.getLogger(getClass)
  private var lastUpdate = System.currentTimeMillis()
  private var lastLogUpdate = System.currentTimeMillis()
  private var lastMessageCount = 0
  private var messageCount = 0
  private var rate: Double = 0

  def update(delta: Int): Unit = {
    messageCount += delta
    computeRate()
  }

  private def computeRate(): Double = {
    val deltaTime = (System.currentTimeMillis() - lastUpdate).toDouble / 1000d
    if (deltaTime >= 1d) {
      val count = messageCount - lastMessageCount
      rate = count.toDouble / deltaTime
      lastMessageCount = messageCount
      lastUpdate = System.currentTimeMillis()

      if(System.currentTimeMillis() - lastLogUpdate >= 3000) {
        logger.info(f"$label: Throughput rate is $rate%.1f")
        lastLogUpdate = System.currentTimeMillis()
      }
    }
    rate
  }

}
