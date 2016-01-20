package com.github.ldaniels528.broadway.cli.io.device

import com.github.ldaniels528.broadway.cli.io.Data

/**
  * Represents an Input Device
  */
trait InputDevice extends Device {

  def hasNext: Boolean

  def read(): Option[Data]

}
