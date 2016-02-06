package com.github.ldaniels528.broadway.core.io.device.impl

import java.io.{BufferedReader, File, FileReader}
import java.util.UUID

import com.github.ldaniels528.broadway.core.io.Scope
import com.github.ldaniels528.broadway.core.io.device.TextReadingSupport.TextInput
import com.github.ldaniels528.broadway.core.io.device.{InputSource, TextReadingSupport}
import com.github.ldaniels528.broadway.core.io.layout._

/**
  * Text File Input Source
  * @author lawrence.daniels@gmail.com
  */
case class TextFileInputSource(id: String, path: String, layout: Layout) extends InputSource with TextReadingSupport {
  private val uuid = UUID.randomUUID()

  override def close(implicit scope: Scope) = scope.discardResource[BufferedReader](uuid).foreach(_.close())

  override def open(implicit scope: Scope) = {
    val file = new File(scope.evaluateAsString(path))
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

  override def readText(implicit scope: Scope) = {
    for {
      reader <- scope.getResource[BufferedReader](uuid)
      line <- Option(reader.readLine)
      _ = updateCount(1)
      offset = getStatistics.offset
    } yield TextInput(line, offset)
  }

}

