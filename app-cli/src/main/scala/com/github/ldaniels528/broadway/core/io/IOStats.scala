package com.github.ldaniels528.broadway.core.io

/**
  * I/O Statistics
  */
case class IOStats(flowId: String, deviceId: String, deviceType: String, count: Long, avgRecordsPerSecond: Double)
