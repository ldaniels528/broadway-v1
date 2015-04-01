package com.ldaniels528.broadway.core.schedules

import scala.concurrent.duration.Duration

/**
 * Represents an Interval Schedule
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
case class IntervalSchedule(id: String, frequency: Duration) extends Scheduling {
  private var lastRunMillis = System.currentTimeMillis()

  /**
   * Indicates whether the given event time is eligible per this scheduler
   * @param eventTime the given event time (in milliseconds)
   * @return true, if the event time is eligible per the scheduler
   */
  override def isEligible(eventTime: Long): Boolean = {
    val isReady = eventTime - lastRunMillis >= frequency.toMillis
    if (isReady) lastRunMillis = System.currentTimeMillis()
    isReady
  }

}
