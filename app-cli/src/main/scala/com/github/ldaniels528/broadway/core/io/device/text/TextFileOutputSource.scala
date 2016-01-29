package com.github.ldaniels528.broadway.core.io.device.text

import java.io.{BufferedWriter, File, FileWriter}
import java.util.UUID

import com.github.ldaniels528.broadway.core.io.Data
import com.github.ldaniels528.broadway.core.io.device.OutputSource
import com.github.ldaniels528.broadway.core.io.layout.Layout
import com.github.ldaniels528.broadway.core.scope.Scope

/**
  * Text File Output Source
  *
  * @author lawrence.daniels@gmail.com
  */
case class TextFileOutputSource(id: String, path: String, layout: Layout) extends OutputSource {
  private val uuid = UUID.randomUUID().toString

  override def close(scope: Scope) = scope.discardResource[BufferedWriter](uuid).foreach(_.close())

  override def open(scope: Scope) = {
    val file = new File(scope.evaluate(path))
    scope ++= Seq(
      "flow.output.id" -> id,
      "flow.output.count" -> (() => getStatistics(scope).count),
      "flow.output.filename" -> file.getName,
      "flow.output.lastModified" -> (() => file.lastModified()),
      "flow.output.length" -> (() => file.length()),
      "flow.output.offset" -> (() => getStatistics(scope).offset),
      "flow.output.path" -> file.getCanonicalPath
    )
    scope.createResource(uuid, new BufferedWriter(new FileWriter(file)))
    ()
  }

  override def write(scope: Scope, data: Data) = {
    scope.getResource[BufferedWriter](uuid) map { writer =>
      writer.write(data.asText)
      writer.newLine()
      updateCount(scope, 1)
    } getOrElse 0
  }

}
