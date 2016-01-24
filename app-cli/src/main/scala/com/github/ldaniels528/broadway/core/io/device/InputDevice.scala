package com.github.ldaniels528.broadway.core.io.device

import com.github.ldaniels528.broadway.core.io.Data

/**
  * Represents an Input Device
  */
trait InputDevice extends Device {

  def read(): Option[Data]

}