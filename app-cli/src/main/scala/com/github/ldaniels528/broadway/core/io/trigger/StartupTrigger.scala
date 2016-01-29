package com.github.ldaniels528.broadway.core.io.trigger

import com.github.ldaniels528.broadway.core.StoryConfig
import com.github.ldaniels528.broadway.core.actors.ProcessingActor
import com.github.ldaniels528.broadway.core.io.flow.Flow
import com.github.ldaniels528.broadway.core.scope.GlobalScope

import scala.concurrent.ExecutionContext

/**
  * Startup Trigger
  */
case class StartupTrigger(id: String, flows: Seq[Flow]) extends Trigger {

  override def execute(story: StoryConfig)(implicit ec: ExecutionContext) = {
    ProcessingActor ! new Runnable {
      override def run() = {
        implicit val rootScope = GlobalScope()
        process(flows zip (flows map (createScope(rootScope, _))))
      }
    }
  }

}
