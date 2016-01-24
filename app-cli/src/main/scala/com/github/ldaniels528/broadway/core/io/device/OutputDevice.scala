package com.github.ldaniels528.broadway.core.io.device

import com.github.ldaniels528.broadway.core.io.Data

/**
  * Represents an Output Device
  */
trait OutputDevice extends Device {

  def write(data: Data): Int

}
