package com.github.ldaniels528.broadway.core.io.layout

import com.github.ldaniels528.broadway.core.RuntimeContext
import com.github.ldaniels528.broadway.core.io.Data
import com.github.ldaniels528.broadway.core.io.device.{OutputDevice, InputDevice}

/**
  * Represents the logic layout of a text format
  */
trait Layout {

  def id: String

  def in(rt: RuntimeContext, device: InputDevice, data: Option[Data]): Seq[Data]

  def out(rt: RuntimeContext, device: OutputDevice, dataSet: Seq[Data], isEOF: Boolean): Seq[Data]

}