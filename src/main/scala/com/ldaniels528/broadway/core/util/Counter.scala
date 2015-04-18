package com.ldaniels528.broadway.core.util

import java.util.concurrent.atomic.AtomicLong

import scala.concurrent.duration.Duration

/**
 * Processed Record Counter
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
class Counter(frequency: Duration)(observer: (Long, Long, Double) => Unit) {
  private val lastUpdate = new AtomicLong(0)
  private var lastSuccessCount = 0L
  private var lastErrorCount = 0L
  private val successes = new AtomicLong(0)
  private val errors = new AtomicLong(0)
  private var rps = 0.0d

  def +=(delta: Int) = produceStats(successes.addAndGet(delta), errors.get)

  def -=(delta: Int) = produceStats(successes.get, errors.addAndGet(delta))

  def recordsPerSecond: Double = rps

  private def produceStats(successes: Long, failures: Long) {
    val currentTimeMillis = System.currentTimeMillis()
    if (!lastUpdate.compareAndSet(0L, currentTimeMillis)) {
      val lastCheckMillis = lastUpdate.get
      val dtime = currentTimeMillis - lastCheckMillis
      if (dtime >= frequency.toMillis && lastUpdate.compareAndSet(lastCheckMillis, currentTimeMillis)) {
        val timeSecs = dtime.toDouble / 1000d
        val deltaSuccess = successes - lastSuccessCount
        val deltaErrors = failures - lastErrorCount
        rps = if (timeSecs == 0.0d) 0.0d else deltaSuccess.toDouble / timeSecs
        lastSuccessCount = successes
        lastErrorCount = failures
        observer(deltaSuccess, deltaErrors, rps)
      }
    }
  }

}
