package com.ldaniels528.broadway.core.util

import scala.concurrent.duration.Duration

/**
 * Processed Record Counter
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
class Counter(frequency: Duration)(observer: (Long, Double) => Unit) {
  private var lastCheckMillis = 0L
  private var lastCount = 0L
  private var count = 0L
  private var rps = 0.0d

  def +=(delta: Int) = {
    count += delta
    produceStats()
  }

  def recordsPerSecond: Double = rps

  private def produceStats(): Unit = {
    if (lastCheckMillis == 0) lastCheckMillis = System.currentTimeMillis()
    else {
      val dtime = System.currentTimeMillis() - lastCheckMillis
      if (dtime >= frequency.toMillis) {
        val timeSecs = dtime.toDouble / 1000d
        val delta = count - lastCount
        rps = if (timeSecs == 0.0d) 0.0d else delta.toDouble / timeSecs
        lastCount = count
        lastCheckMillis = System.currentTimeMillis()
        observer(delta, rps)
      }
    }
  }

}
