package com.github.ldaniels528.broadway.core.io.layout

import com.github.ldaniels528.broadway.core.RuntimeContext
import com.github.ldaniels528.broadway.core.io.Data
import com.github.ldaniels528.broadway.core.io.device.{BinaryReading, BinaryWriting}

/**
  * Represents a Binary Layout
  */
trait BinaryLayout extends Layout {

  def in(rt: RuntimeContext, device: BinaryReading, binary: Option[Array[Byte]]): Seq[Data]

  def out(rt: RuntimeContext, device: BinaryWriting, dataSet: Seq[Data], isEOF: Boolean): Option[Int]

}
