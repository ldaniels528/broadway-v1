package com.ldaniels528.broadway.core.narrative

import com.ldaniels528.broadway.core.triggers.location.Location
import com.ldaniels528.broadway.core.triggers.schedules.Scheduling
import com.ldaniels528.broadway.core.triggers.Trigger

import scala.beans.BeanProperty

/**
 * Represents an anthology; a collection of narratives
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
case class Anthology(@BeanProperty id: String,
                     @BeanProperty locations: Seq[Location],
                     @BeanProperty propertySets: Seq[PropertySet],
                     @BeanProperty schedules: Seq[Scheduling],
                     @BeanProperty narratives: Seq[NarrativeDescriptor],
                     @BeanProperty triggers: Seq[Trigger])