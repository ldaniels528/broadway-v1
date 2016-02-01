package com.github.ldaniels528.broadway.core.io.layout.json

import java.io.File

import com.github.ldaniels528.broadway.core.io._
import com.github.ldaniels528.broadway.core.io.device.{InputSource, OutputSource}
import com.github.ldaniels528.broadway.core.io.layout.Layout.InputSet
import com.github.ldaniels528.broadway.core.io.layout._
import com.github.ldaniels528.broadway.core.util.AvroConversion
import AvroConversion._
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

  override def read(device: InputSource)(implicit scope: Scope): InputSet = ???

  override def write(device: OutputSource, inputSet: InputSet)(implicit scope: Scope): Unit = ???
}

/**
  * Avro Layout Companion Object
  */
object AvroLayout {

  def apply(id: String, fields: FieldSet, file: File) = {
    new AvroLayout(id, fields, Source.fromFile(file).getLines() mkString "\n")
  }

}