package com.ldaniels528.broadway.core.triggers

import com.ldaniels528.broadway.core.narrative.NarrativeDescriptor
import com.ldaniels528.broadway.core.resources.Resource
import com.ldaniels528.broadway.core.schedules.Scheduling

/**
 * Represents a Narrative Trigger
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
case class Trigger(narrative: NarrativeDescriptor, schedule: Scheduling, resource: Option[Resource] = None) {

  def isReady(eventTime: Long): Boolean = schedule.isEligible(eventTime)

}
