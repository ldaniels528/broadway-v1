package com.github.ldaniels528.broadway.core.io.device

import java.io.{BufferedReader, File, FileReader}
import java.util.UUID

import com.github.ldaniels528.broadway.core.io.Scope
import com.github.ldaniels528.broadway.core.io.device.TextRecordInputSource.TextInput
import com.github.ldaniels528.broadway.core.io.layout._

/**
  * Text File Input Source
  *
  * @author lawrence.daniels@gmail.com
  */
case class TextFileInputSource(id: String, path: String, layout: Layout) extends InputSource with TextRecordInputSource {
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

  override def readLine(scope: Scope) = {
    for {
      reader <- scope.getResource[BufferedReader](uuid)
      line <- Option(reader.readLine)
      _ = updateCount(scope, 1)
      offset = getStatistics(scope).offset
    } yield TextInput(line, offset)
  }

}

