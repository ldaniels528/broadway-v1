package com.github.ldaniels528.broadway.core.io.device.text

import java.io.{BufferedReader, File, FileReader}
import java.util.UUID

import com.github.ldaniels528.broadway.core.io.Data
import com.github.ldaniels528.broadway.core.io.device.{InputSource, TextRecordInputSource}

import com.github.ldaniels528.broadway.core.io.layout._
import com.github.ldaniels528.broadway.core.io.layout.text.FixedLengthFieldSet
import com.github.ldaniels528.broadway.core.scope.Scope

/**
  * Text File Input Source
  *
  * @author lawrence.daniels@gmail.com
  */
case class TextFileInputSource(id: String, path: String, layout: Layout) extends InputSource with TextRecordInputSource {
  private val fieldSet = FixedLengthFieldSet(Seq(Field(name = "line")))
  private val uuid = UUID.randomUUID().toString

  val templateRecord = DelimitedRecord(
    id = "delimited_data",
    delimiter = "\t",
    `type` = RecordTypes.BODY,
    fields = Seq(
      Field(name = "symbol", `type` = DataTypes.STRING),
      Field(name = "description", `type` = DataTypes.STRING)
    ))

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
    data foreach (_ => updateCount(scope, 1))
    data
  }

  override def readRecord(implicit scope: Scope) = {
    val reader = scope.getResource[BufferedReader](uuid)
    val record = reader.flatMap(r => Option(r.readLine)).map(templateRecord.copy().fromLine)
    record foreach (_ => updateCount(scope, 1))
    record
  }

}

