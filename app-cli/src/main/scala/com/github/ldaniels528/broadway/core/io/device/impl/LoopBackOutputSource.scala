package com.github.ldaniels528.broadway.core.io.device.impl

import com.github.ldaniels528.broadway.core.io.Scope
import com.github.ldaniels528.broadway.core.io.device.{DataSet, OutputSource}
import com.github.ldaniels528.broadway.core.io.layout.Layout
import com.github.ldaniels528.broadway.core.io.record.Record

/**
  * Loop-back OutputSource
  * @author lawrence.daniels@gmail.com
  */
case class LoopBackOutputSource(id: String, layout: Layout) extends OutputSource {

  override def writeRecord(record: Record, dataSet: DataSet)(implicit scope: Scope) = 1

  override def close(implicit scope: Scope) = ()

  override def open(implicit scope: Scope) = {
    scope ++= Seq(
      "flow.output.id" -> id,
      "flow.output.count" -> (() => getStatistics.count),
      "flow.output.offset" -> (() => getStatistics.offset)
    )
    ()
  }

}
