package com.github.ldaniels528.broadway.core.io.layout

import com.github.ldaniels528.broadway.core.io.device.{InputSource, OutputSource}
import com.github.ldaniels528.broadway.core.io.layout.Layout.InputSet
import com.github.ldaniels528.broadway.core.io.{Data, Scope}

/**
  * Represents the logic layout of a text format
  */
trait Layout {

  def id: String

  def read(device: InputSource)(implicit scope: Scope): InputSet

  def write(device: OutputSource, inputSet: InputSet)(implicit scope: Scope): Unit

  @deprecated
  def in(scope: Scope, device: InputSource, data: Option[Data]): Seq[Data]

  @deprecated
  def out(scope: Scope, device: OutputSource, dataSet: Seq[Data], isEOF: Boolean): Seq[Data]

}

/**
  * Layout Companion Object
  */
object Layout {

  /**
    * Input Set
    */
  case class InputSet(records: Seq[Record], offset: Long, isEOF: Boolean)

}