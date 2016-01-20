package com.github.ldaniels528.broadway.core.io.device

import com.github.ldaniels528.broadway.core.io.Data
import com.github.ldaniels528.broadway.core.io.layout.OutputLayout

/**
  * Represents an Output Device
  */
trait OutputDevice extends Device {

  def layout: OutputLayout

  def write(data: Data): Int

}
