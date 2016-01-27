package com.github.ldaniels528.broadway.core

import com.github.ldaniels528.broadway.core.io.trigger.Trigger

/**
  * Story Configuration
  *
  * @author lawrence.daniels@gmail.com
  */
case class StoryConfig(id: String, triggers: Seq[Trigger])