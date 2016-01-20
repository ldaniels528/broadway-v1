package com.github.ldaniels528.broadway.core.io.device

/**
  * I/O Statistic Generation
  *
  * @author lawrence.daniels@gmail.com
  */
trait StatisticsGeneration {
  private var firstUpdate: Long = System.currentTimeMillis()
  private var lastUpdate: Long = System.currentTimeMillis()
  private var lastRpsUpdate: Long = 0
  private var lastCount: Long = 0

  var count: Long = 0
  var rps: Double = 0

  def avgRecordsPerSecond = count / ((lastUpdate - firstUpdate) / 1000d)

  def updateCount(delta: Int) = {
    count += delta
    lastUpdate = System.currentTimeMillis()

    val diff = (lastUpdate - lastRpsUpdate) / 1000d
    if (lastRpsUpdate == 0 || diff >= 1) {
      if (lastRpsUpdate == 0) firstUpdate = lastUpdate
      rps = if (lastRpsUpdate == 0) count else (count - lastCount) / diff
      lastCount = count
      lastRpsUpdate = lastUpdate
    }
    delta
  }

}
