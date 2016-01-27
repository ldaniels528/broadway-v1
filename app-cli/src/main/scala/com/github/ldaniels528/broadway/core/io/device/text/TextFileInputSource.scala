package com.github.ldaniels528.broadway.core.io.device.text

import java.io.{BufferedReader, File, FileReader}
import java.util.UUID

import com.github.ldaniels528.broadway.core.io.Data
import com.github.ldaniels528.broadway.core.io.device.InputSource
import com.github.ldaniels528.broadway.core.io.device.text.TextFileInputSource.getProperties
import com.github.ldaniels528.broadway.core.io.layout.text.fields.TextLineFieldSet
import com.github.ldaniels528.broadway.core.scope.Scope

/**
  * Text File Input Source
  *
  * @author lawrence.daniels@gmail.com
  */
case class TextFileInputSource(id: String, path: String) extends InputSource {
  private val fieldSet = TextLineFieldSet("line")
  private val uuid = UUID.randomUUID().toString

  override def close(scope: Scope) = {
    scope.discardResource[BufferedReader](uuid).foreach(_.close())
  }

  override def open(scope: Scope) = {
    val file = new File(scope.evaluate(path))
    scope.putIfAbsent(getProperties(file))
    scope.createResource(uuid, new BufferedReader(new FileReader(file)))
    ()
  }

  override def read(scope: Scope) = {
    val reader = scope.getResource[BufferedReader](uuid)
    val data = reader.flatMap(r => Option(r.readLine)).map(Data(fieldSet, _))
    updateCount(scope, 1)
    data
  }

}

/**
  * Text File Input IOSource Companion Object
  */
object TextFileInputSource {

  def getProperties(file: File): Seq[(String, Any)] = {
    Seq(
      "flow.input.filename" -> file.getName,
      "flow.input.lastModified" -> (() => file.lastModified()),
      "flow.input.length" -> (() => file.length()),
      "flow.input.path" -> file.getCanonicalPath
    )
  }

}
