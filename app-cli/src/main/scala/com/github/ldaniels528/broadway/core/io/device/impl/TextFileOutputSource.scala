package com.github.ldaniels528.broadway.core.io.device.impl

import java.io.{BufferedWriter, File, FileWriter}
import java.util.UUID

import com.github.ldaniels528.broadway.core.io.Scope
import com.github.ldaniels528.broadway.core.io.device.OutputSource
import com.github.ldaniels528.broadway.core.io.layout._
import com.github.ldaniels528.broadway.core.io.record.Record
import com.ldaniels528.commons.helpers.OptionHelper._

/**
  * Text File Output Source
  * @author lawrence.daniels@gmail.com
  */
case class TextFileOutputSource(id: String, path: String, layout: Layout) extends OutputSource {
  private val uuid = UUID.randomUUID()

  override def close(implicit scope: Scope) = {
    scope.discardResource[BufferedWriter](uuid).foreach(_.close())
  }

  override def open(implicit scope: Scope) = {
    val file = new File(scope.evaluateAsString(path))
    scope ++= Seq(
      "flow.output.id" -> id,
      "flow.output.count" -> (() => getStatistics.count),
      "flow.output.filename" -> file.getName,
      "flow.output.lastModified" -> (() => file.lastModified()),
      "flow.output.length" -> (() => file.length()),
      "flow.output.offset" -> (() => getStatistics.offset),
      "flow.output.path" -> file.getCanonicalPath
    )
    scope.createResource(uuid, new BufferedWriter(new FileWriter(file)))
    ()
  }

  override def writeRecord(record: Record)(implicit scope: Scope) = {
    scope.getResource[BufferedWriter](uuid) map { writer =>
      writer.write(record.convertToText)
      writer.newLine()
      updateCount(1)
    } orDie s"Text file output source '$id' has not been opened"
  }

}
