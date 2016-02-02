package com.github.ldaniels528.broadway.core.io.layout

import com.github.ldaniels528.broadway.core.io.Scope
import com.github.ldaniels528.broadway.core.io.device.{InputSource, OutputSource}
import com.github.ldaniels528.broadway.core.io.layout.Layout.InputSet
import com.github.ldaniels528.broadway.core.io.record.Record

/**
  * Represents the logic layout of a text format
  * @author lawrence.daniels@gmail.com
  */
trait Layout {

  def id: String

  def read(device: InputSource)(implicit scope: Scope): InputSet

  def write(device: OutputSource, inputSet: InputSet)(implicit scope: Scope): Unit

}

/**
  * Layout Companion Object
  * @author lawrence.daniels@gmail.com
  */
object Layout {

  /**
    * Input Set
    */
  case class InputSet(records: Seq[Record], offset: Long, isEOF: Boolean)

}