package com.github.ldaniels528.broadway.core.io.device.impl

import com.github.ldaniels528.broadway.core.io.Scope
import com.github.ldaniels528.broadway.core.io.device.OutputSource
import com.github.ldaniels528.broadway.core.io.record.Record
import com.ldaniels528.commons.helpers.OptionHelper._

/**
  * Composite Output Source (Synchronous implementation)
  * @author lawrence.daniels@gmail.com
  */
case class CompositeOutputSource(id: String, devices: Seq[OutputSource]) extends OutputSource {

  val layout = devices.headOption.map(_.layout) orDie "No output devices configured"

  override def close(implicit scope: Scope) = devices.foreach(_.close(scope))

  override def open(implicit scope: Scope) = devices.foreach(_.open(scope))

  override def writeRecord(record: Record)(implicit scope: Scope) = devices.map(_.writeRecord(record)).sum

}
