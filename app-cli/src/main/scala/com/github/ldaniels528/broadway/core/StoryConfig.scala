package com.github.ldaniels528.broadway.core

import com.github.ldaniels528.broadway.core.io.archive.Archive
import com.github.ldaniels528.broadway.core.io.device.DataSource
import com.github.ldaniels528.broadway.core.io.filters.Filter
import com.github.ldaniels528.broadway.core.io.layout.Layout
import com.github.ldaniels528.broadway.core.io.trigger.Trigger

/**
  * Story Configuration
  * @author lawrence.daniels@gmail.com
  */
case class StoryConfig(id: String,
                       archives: Seq[Archive] = Nil,
                       devices: Seq[DataSource] = Nil,
                       filters: Seq[(String, Filter)] = Nil,
                       layouts: Seq[Layout] = Nil,
                       properties: Seq[(String, String)] = Nil,
                       triggers: Seq[Trigger] = Nil)