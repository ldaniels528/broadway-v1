package com.ldaniels528.broadway.core.triggers.schedules

import scala.concurrent.duration._

/**
 * Represents an Daily Schedule
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
case class DailySchedule(id: String) extends Scheduling {
  private var lastRunMillis = 0L

  /**
   * Indicates whether the given event time is eligible per this scheduler
   * @param eventTime the given event time (in milliseconds)
   * @return true, if the event time is eligible per the scheduler
   */
  override def isEligible(eventTime: Long): Boolean = {
    val isReady = eventTime - lastRunMillis >= 24.hours.toMillis
    if (isReady) lastRunMillis = System.currentTimeMillis()
    isReady
  }

}