package com.github.ldaniels528.broadway.core.io.device.text

import com.github.ldaniels528.broadway.core.io.device.OutputDevice
import com.github.ldaniels528.broadway.core.io.layout.text.TextLayout

/**
  * Represents a device capable for persisting text
  */
trait TextWriting {
  self: OutputDevice =>

  def layout: TextLayout

  def offset: Long

  def writeLine(line: String): Int

}
