package com.github.ldaniels528.broadway.core.opcode.trigger

import com.github.ldaniels528.broadway.cli.actors.ProcessingActor
import com.github.ldaniels528.broadway.core.ETLConfig
import com.github.ldaniels528.broadway.core.opcode.flow.Flow

import scala.concurrent.ExecutionContext

/**
  * Startup Trigger
  */
case class StartupTrigger(id: String, flows: Seq[Flow]) extends Trigger {

  override def execute(config: ETLConfig)(implicit ec: ExecutionContext) = {
    ProcessingActor ! new Runnable {
      override def run(): Unit = process(flows)
    }
  }

}
