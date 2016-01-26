package com.github.ldaniels528.broadway.core.opcode.flow

import com.github.ldaniels528.broadway.core.io.device.{OutputDevice, InputDevice, Device}
import com.github.ldaniels528.broadway.core.opcode.OpCode

/**
  * ETL Process Flow
  *
  * @author lawrence.daniels@gmail.com
  */
trait Flow extends OpCode[Unit] {

  def id: String

  def devices: Seq[Device]

  def input: InputDevice

  def output: OutputDevice

}
