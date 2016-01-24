package com.github.ldaniels528.broadway.core.io.device

import com.github.ldaniels528.broadway.core.io.layout.BinaryLayout

/**
  * Binary Writing
  */
trait BinaryWriting {

  def layout: BinaryLayout

  def writeBytes(data: Array[Byte]): Int

}
