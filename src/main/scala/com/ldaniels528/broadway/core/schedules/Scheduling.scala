package com.ldaniels528.broadway.core.schedules

/**
 * Broadway Scheduling Trait
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
trait Scheduling {

  /**
   * Returns the unique identifier 
   * @return the unique identifier 
   */
  def id: String

  /**
   * Indicates whether the given event time is eligible per this scheduler
   * @param eventTime the given event time (in milliseconds)
   * @return true, if the event time is eligible per the scheduler
   */
  def isEligible(eventTime: Long): Boolean
  
}
