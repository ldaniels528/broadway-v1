package com.ldaniels528.broadway.core.util

/**
 * Throughput Calculator
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
class ThroughputCalculator(listener: Double => Unit) {
  private var lastUpdateMillis = System.currentTimeMillis()
  private var lastMessageCount = 0
  private var messageCount = 0
  private var messagesPerSecond: Double = 0

  def update(delta: Int): Unit = {
    messageCount += delta
    computeRate()
  }

  private def computeRate() {
    val deltaTime = (System.currentTimeMillis() - lastUpdateMillis).toDouble / 1000d
    if (deltaTime >= 1d) {
      val count = messageCount - lastMessageCount
      messagesPerSecond = count.toDouble / deltaTime
      listener(messagesPerSecond)
      lastMessageCount = messageCount
      lastUpdateMillis = System.currentTimeMillis()
    }
  }

}
