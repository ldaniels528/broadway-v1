package com.ldaniels528.broadway.core.util

import java.util.concurrent.atomic.AtomicLong

import scala.concurrent.duration.Duration

/**
 * Processed Record Counter
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
class Counter(frequency: Duration)(observer: (Long, Double) => Unit) {
  private var lastCheckMillis = 0L
  private var lastCount = 0L
  private val count = new AtomicLong(0)
  private var rps = 0.0d

  def +=(delta: Int) = produceStats(count.addAndGet(delta))

  def recordsPerSecond: Double = rps

  private def produceStats(total: Long): Unit = {
    if (lastCheckMillis == 0) lastCheckMillis = System.currentTimeMillis()
    else {
      val dtime = System.currentTimeMillis() - lastCheckMillis
      if (dtime >= frequency.toMillis) {
        val timeSecs = dtime.toDouble / 1000d
        val delta = total - lastCount
        rps = if (timeSecs == 0.0d) 0.0d else delta.toDouble / timeSecs
        lastCount = total
        lastCheckMillis = System.currentTimeMillis()
        observer(delta, rps)
      }
    }
  }

}
