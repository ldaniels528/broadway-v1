package com.github.ldaniels528.broadway.core.flow

import com.github.ldaniels528.broadway.core.RuntimeContext
import com.github.ldaniels528.broadway.core.io.device.Device

/**
  * ETL Process Flow
  *
  * @author lawrence.daniels@gmail.com
  */
trait Flow {

  def id: String

  def devices: Seq[Device]

  def execute(rt: RuntimeContext): Unit

}
