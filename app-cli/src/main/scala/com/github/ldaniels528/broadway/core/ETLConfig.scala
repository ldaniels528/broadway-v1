package com.github.ldaniels528.broadway.core

import com.github.ldaniels528.broadway.core.opcode.trigger.Trigger

/**
  * ETL Configuration
  *
  * @author lawrence.daniels@gmail.com
  */
case class ETLConfig(id: String, triggers: Seq[Trigger])