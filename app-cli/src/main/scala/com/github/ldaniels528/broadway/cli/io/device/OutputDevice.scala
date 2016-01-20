package com.github.ldaniels528.broadway.cli.io.device

import com.github.ldaniels528.broadway.cli.io.Data
import com.github.ldaniels528.broadway.cli.io.layout.OutputLayout

/**
  * Represents an Output Device
  */
trait OutputDevice extends Device {

  def layout: OutputLayout

  def write(data: Data): Int

}
