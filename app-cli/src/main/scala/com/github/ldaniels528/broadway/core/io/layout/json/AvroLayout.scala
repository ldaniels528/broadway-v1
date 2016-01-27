package com.github.ldaniels528.broadway.core.io.layout.json

import java.io.File

import com.github.ldaniels528.broadway.core.io._
import com.github.ldaniels528.broadway.core.io.device.{InputSource, OutputSource}
import com.github.ldaniels528.broadway.core.io.layout._
import com.github.ldaniels528.broadway.core.io.layout.json.AvroConversion._
import com.github.ldaniels528.broadway.core.scope.Scope
import org.apache.avro.Schema

import scala.io.Source
import scala.language.postfixOps

/**
  * Represents an Avro Layout
  */
case class AvroLayout(id: String, fieldSet: FieldSet, schemaString: String) extends Layout {
  private val schema = new Schema.Parser().parse(schemaString)

  override def in(scope: Scope, device: InputSource, data: Option[Data]) = {
    data match {
      case Some(theData) => Seq(Data(fieldSet, transcodeAvroBytesToAvroJson(schema, theData.asBytes)))
      case None => Nil
    }
  }

  override def out(scope: Scope, device: OutputSource, dataSet: Seq[Data], isEOF: Boolean) = {
    dataSet map (_.asAvroBytes(schema)) map (Data(fieldSet, _))
  }

}

/**
  * Avro Layout Companion Object
  */
object AvroLayout {

  def apply(id: String, fields: FieldSet, file: File) = {
    new AvroLayout(id, fields, Source.fromFile(file).getLines() mkString "\n")
  }

}