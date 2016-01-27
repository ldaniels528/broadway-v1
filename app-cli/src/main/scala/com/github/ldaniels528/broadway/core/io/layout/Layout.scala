package com.github.ldaniels528.broadway.core.io.layout

import com.github.ldaniels528.broadway.core.io.Data
import com.github.ldaniels528.broadway.core.io.device.{InputSource, OutputSource}
import com.github.ldaniels528.broadway.core.scope.Scope

/**
  * Represents the logic layout of a text format
  */
trait Layout {

  def id: String

  def in(scope: Scope, device: InputSource, data: Option[Data]): Seq[Data]

  def out(scope: Scope, device: OutputSource, dataSet: Seq[Data], isEOF: Boolean): Seq[Data]

}