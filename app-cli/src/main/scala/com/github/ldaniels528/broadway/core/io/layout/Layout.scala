package com.github.ldaniels528.broadway.core.io.layout

import com.github.ldaniels528.broadway.core.io.Scope
import com.github.ldaniels528.broadway.core.io.device.{InputSource, OutputSource}
import com.github.ldaniels528.broadway.core.io.layout.Layout.InputSet

/**
  * Represents the logic layout of a text format
  */
trait Layout {

  def id: String

  def read(device: InputSource)(implicit scope: Scope): InputSet

  def write(device: OutputSource, inputSet: InputSet)(implicit scope: Scope): Unit

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