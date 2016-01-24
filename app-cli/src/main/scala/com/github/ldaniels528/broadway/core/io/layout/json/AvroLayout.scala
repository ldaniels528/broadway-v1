package com.github.ldaniels528.broadway.core.io.layout.json

import java.io.File

import com.github.ldaniels528.broadway.core.RuntimeContext
import com.github.ldaniels528.broadway.core.io._
import com.github.ldaniels528.broadway.core.io.device.{BinaryReading, BinaryWriting}
import com.github.ldaniels528.broadway.core.io.layout._
import com.github.ldaniels528.broadway.core.io.layout.json.AvroConversion._
import com.github.ldaniels528.broadway.core.io.layout.text.fields.JsonFieldSet
import org.apache.avro.Schema

import scala.io.Source
import scala.language.postfixOps

/**
  * Represents an Avro Layout
  */
case class AvroLayout(id: String, fieldSet: FieldSet, schemaString: String) extends Layout with BinaryLayout {
  private val schema = new Schema.Parser().parse(schemaString)

  override def in(rt: RuntimeContext, device: BinaryReading, binary: Option[Array[Byte]]) = {
    binary match {
      case Some(bytes) => Seq(Data(transcodeAvroBytesToAvroJson(schema, bytes)))
      case None => Nil
    }
  }

  override def out(rt: RuntimeContext, device: BinaryWriting, dataSet: Seq[Data], isEOF: Boolean) = {
    val binaries = dataSet map {
      case ArrayData(values) => transcodeJsonToAvroBytes(JsonFieldSet.toJsonText(fieldSet.fields.map(_.name) zip values), schema)
      case ByteData(bytes) => bytes //transcodeAvroBytesToAvroJson(schema, bytes)
      case JsonData(js) => transcodeJsonToAvroBytes(js.toString(), schema)
      case TextData(s) => transcodeJsonToAvroBytes(s, schema)
      case data =>
        throw new IllegalStateException(s"Unrecognized data type '$data' for encoding (${Option(data).map(_.getClass.getName).orNull})")
    }

    Option(binaries map { bytes =>
      device.writeBytes(bytes)
    } sum)
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