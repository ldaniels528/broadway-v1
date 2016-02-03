package com.github.ldaniels528.broadway.core

import com.github.ldaniels528.broadway.core.io.filters.Filter
import com.github.ldaniels528.broadway.core.io.trigger.Trigger

/**
  * Story Configuration
  * @author lawrence.daniels@gmail.com
  */
case class StoryConfig(id: String,
                       filters: Seq[(String, Filter)],
                       properties: Seq[(String, String)],
                       triggers: Seq[Trigger])