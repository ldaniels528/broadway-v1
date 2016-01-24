package com.github.ldaniels528.broadway.core.io.device.text

import com.github.ldaniels528.broadway.core.io.device.InputDevice
import com.github.ldaniels528.broadway.core.io.layout.text.TextLayout

/**
  * Represents a device capable for retrieving text
  */
trait TextReading {
  self: InputDevice =>

  def layout: TextLayout

  def offset: Long

  def readLine: Option[String]

}
