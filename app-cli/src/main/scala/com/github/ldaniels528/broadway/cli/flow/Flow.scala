package com.github.ldaniels528.broadway.cli.flow

import com.github.ldaniels528.broadway.cli.EtlConfig
import com.github.ldaniels528.broadway.cli.io.device.Device

/**
  * ETL Process Flow
  *
  * @author lawrence.daniels@gmail.com
  */
trait Flow {

  def id: String

  def devices: Seq[Device]

  def execute(config: EtlConfig): Unit

}
