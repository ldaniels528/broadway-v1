package com.github.ldaniels528.broadway.core.io.flow

import com.github.ldaniels528.broadway.core.io.OpCode
import com.github.ldaniels528.broadway.core.io.device.{OutputSource, InputSource, IOSource}

/**
  * ETL Process Flow
  *
  * @author lawrence.daniels@gmail.com
  */
trait Flow extends OpCode[Unit] {

  def id: String

  def devices: Seq[IOSource]

  def input: InputSource

  def output: OutputSource

}
