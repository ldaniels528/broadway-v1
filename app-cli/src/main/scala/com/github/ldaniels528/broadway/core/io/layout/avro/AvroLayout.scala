package com.github.ldaniels528.broadway.core.io.layout.avro

import java.io.File

import com.github.ldaniels528.broadway.core.io._
import com.github.ldaniels528.broadway.core.io.layout._
import com.github.ldaniels528.broadway.core.io.layout.avro.AvroConversion._
import com.github.ldaniels528.broadway.core.io.layout.text.fields.JsonFieldSet
import org.apache.avro.Schema

import scala.io.Source

/**
  * Represents an Avro Layout
  */
case class AvroLayout(id: String, fieldSet: FieldSet, schemaString: String) extends InputLayout with OutputLayout {
  private val schema = new Schema.Parser().parse(schemaString)

  override def decode(recordNo: Long, text: String) = Option {
    Data(transcodeAvroBytesToAvroJson(schema, text.getBytes))
  }

  override def encode(recordNo: Long, data: Data) = Option {
    data match {
      case ArrayData(values) => new String(transcodeJsonToAvroBytes(JsonFieldSet.toJsonText(fieldSet.fields.map(_.name) zip values), schema))
      case ByteData(bytes) => transcodeAvroBytesToAvroJson(schema, bytes)
      case JsonData(js) => new String(transcodeJsonToAvroBytes(js.toString(), schema))
      case TextData(s) => new String(transcodeJsonToAvroBytes(s, schema))
      case _ =>
        throw new IllegalStateException(s"Unrecognized data type '$data' for encoding (${Option(data).map(_.getClass.getName).orNull})")
    }
  }

  override def header: Seq[Division] = Nil

  override def footer: Seq[Division] = Nil

}

/**
  * Avro Layout Companion Object
  */
object AvroLayout {

  def apply(id: String, fields: FieldSet, file: File): AvroLayout = {
    new AvroLayout(id, fields, Source.fromFile(file).getLines() mkString "\n")
  }

}