package com.github.ldaniels528.broadway.core.io.trigger.impl

import com.github.ldaniels528.broadway.core.StoryConfig
import com.github.ldaniels528.broadway.core.actors.ProcessingActor
import com.github.ldaniels528.broadway.core.io.flow.Flow
import com.github.ldaniels528.broadway.core.io.trigger.Trigger

import scala.concurrent.ExecutionContext

/**
  * Startup Trigger
  * @author lawrence.daniels@gmail.com
  */
case class StartupTrigger(id: String, flows: Seq[Flow]) extends Trigger {

  override def execute(story: StoryConfig)(implicit ec: ExecutionContext) = {
    ProcessingActor ! new Runnable {
      override def run() = {
        process(flows zip (flows map (createScope(story, _))))
      }
    }
  }

}
