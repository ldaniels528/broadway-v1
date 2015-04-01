package com.ldaniels528.broadway.core.schedules

/**
 * Represents a non-running schedule
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
case class NonRunningSchedule(id: String) extends Scheduling {

  override def isEligible(eventTime: Long) = false
}
