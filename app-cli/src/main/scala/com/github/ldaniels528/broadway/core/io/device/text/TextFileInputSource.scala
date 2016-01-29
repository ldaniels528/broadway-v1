package com.github.ldaniels528.broadway.core.io.device.text

import java.io.{BufferedReader, File, FileReader}
import java.util.UUID

import com.github.ldaniels528.broadway.core.io.Data
import com.github.ldaniels528.broadway.core.io.device.InputSource
import com.github.ldaniels528.broadway.core.io.layout.Layout
import com.github.ldaniels528.broadway.core.io.layout.text.fields.TextLineFieldSet
import com.github.ldaniels528.broadway.core.scope.Scope

/**
  * Text File Input Source
  *
  * @author lawrence.daniels@gmail.com
  */
case class TextFileInputSource(id: String, path: String, layout: Layout) extends InputSource {
  private val fieldSet = TextLineFieldSet("line")
  private val uuid = UUID.randomUUID().toString

  override def close(scope: Scope) = scope.discardResource[BufferedReader](uuid).foreach(_.close())

  override def open(scope: Scope) = {
    val file = new File(scope.evaluate(path))
    scope ++= Seq(
      "flow.input.id" -> id,
      "flow.input.count" -> (() => getStatistics(scope).count),
      "flow.input.filename" -> file.getName,
      "flow.input.lastModified" -> (() => file.lastModified()),
      "flow.input.length" -> (() => file.length()),
      "flow.input.offset" -> (() => getStatistics(scope).offset),
      "flow.input.path" -> file.getCanonicalPath
    )
    scope.createResource(uuid, new BufferedReader(new FileReader(file)))
    ()
  }

  override def read(scope: Scope) = {
    val reader = scope.getResource[BufferedReader](uuid)
    val data = reader.flatMap(r => Option(r.readLine)).map(Data(fieldSet, _))
    data foreach(_ => updateCount(scope, 1))
    data
  }

}

