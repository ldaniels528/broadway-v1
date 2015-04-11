package com.ldaniels528.broadway.core.triggers.schedules

/**
 * Represents a Schedule for an event that should only occur once
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
case class ScheduleOnce(id: String) extends Scheduling {
  private var once = true

  /**
   * Indicates whether the given event time is eligible per this scheduler
   * @param eventTime the given event time (in milliseconds)
   * @return true, if the event time is eligible per the scheduler
   */
  override def isEligible(eventTime: Long): Boolean = {
    val state = once
    once = false
    state
  }

}
