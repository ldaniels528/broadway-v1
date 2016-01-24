package com.github.ldaniels528.broadway.core.io.device

import com.github.ldaniels528.broadway.core.RuntimeContext

/**
  * Binary Reading
  */
trait BinaryReading {

  def read(rt: RuntimeContext): Option[Array[Byte]]

}
