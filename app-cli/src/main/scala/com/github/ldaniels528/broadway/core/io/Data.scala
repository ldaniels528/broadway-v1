package com.github.ldaniels528.broadway.core.io

import com.github.ldaniels528.broadway.core.io.layout.FieldSet
import com.github.ldaniels528.broadway.core.util.AvroConversion
import AvroConversion._
import org.apache.avro.Schema
import play.api.libs.json.{JsValue, Json}

import scala.io.Source
import scala.language.postfixOps

/**
  * Represents a unit of data
  */
trait Data {

  def fieldSet: FieldSet

}

/**
  * Data Companion Object
  *
  * @author lawrence.daniels@gmail.com
  */
object Data {

  def apply(fieldSet: FieldSet, bytes: Array[Byte]): Data = ByteArrayData(fieldSet, bytes)

  def apply(fieldSet: FieldSet, js: JsValue): Data = JsonData(fieldSet, js)

  def apply(fieldSet: FieldSet, value: String): Data = TextData(fieldSet, value)

  def apply(fieldSet: FieldSet, values: Seq[String]): Data = ArrayData(fieldSet, values)

  /**
    * Represents a binary data array
    *
    * @param bytes the given byte array
    */
  case class ByteArrayData(fieldSet: FieldSet, bytes: Array[Byte]) extends Data

  /**
    * Represents collection of data
    *
    * @param values the given array of string
    */
  case class ArrayData(fieldSet: FieldSet, values: Seq[String]) extends Data

  /**
    * Represents JSON data
    *
    * @param js the given [[JsValue JSON object]]
    */
  case class JsonData(fieldSet: FieldSet, js: JsValue) extends Data

  /**
    * Represents text data
    *
    * @param value the given string value
    */
  case class TextData(fieldSet: FieldSet, value: String) extends Data

  /**
    * Data Enrichment
    *
    * @param data the given [[Data data]]
    */
  implicit class DataEnrichment(val data: Data) extends AnyVal {

    def asAvroBytes(schema: Schema) = data match {
      case ad@ArrayData(fields, values) => transcodeJsonToAvroBytes(ad.asJson.toString(), schema)
      case ByteArrayData(_, bytes) => transcodeJsonToAvroBytes(new String(bytes), schema)
      case JsonData(_, js) => transcodeJsonToAvroBytes(js.toString(), schema)
      case TextData(_, s) => transcodeJsonToAvroBytes(s, schema)
      case _ =>
        throw new IllegalStateException(s"Unrecognized data type '$data' for encoding (${Option(data).map(_.getClass.getName).orNull})")
    }

    def asBytes: Array[Byte] = data match {
      case ArrayData(_, values) => values.mkString.getBytes
      case ByteArrayData(_, bytes) => bytes
      case JsonData(_, js) => js.toString().getBytes
      case TextData(_, value) => value.getBytes
      case _ =>
        throw new IllegalArgumentException(s"Unsupported data type '$data' (${Option(data).map(_.getClass.getName).orNull})")
    }

    def asJson = data match {
      case ByteArrayData(_, bytes) => Json.parse(new String(bytes))
      case JsonData(_, js) => js
      case TextData(_, value) => Json.parse(value)
      case _ =>
        val mapping = Map(data.asTuples: _*)
        val typedValues = data.fieldSet.fields map { field =>
          field.name -> DataConversion.convertToJson(mapping.get(field.name), field.`type`.toTypeName)
        }
        typedValues.foldLeft(Json.obj()) { case (js, (k, v)) => js ++ Json.obj(k -> v) }
    }

    def asText: String = data match {
      case ArrayData(_, values) => values.mkString
      case ByteArrayData(_, bytes) => new String(bytes)
      case JsonData(_, js) => js.toString()
      case TextData(_, value) => value
      case _ =>
        throw new IllegalArgumentException(s"Unsupported data type '$data' (${Option(data).map(_.getClass.getName).orNull})")
    }

    def asTuples: Seq[(String, String)] = data.fieldSet.fields.map(_.name) zip data.asValues

    def asValues: Seq[String] = data match {
      case ArrayData(_, values) => values
      case ByteArrayData(_, bytes) => Source.fromBytes(bytes).getLines().toSeq
      case JsonData(_, js) => Source.fromString(js.toString()).getLines().toSeq
      case TextData(_, value) => Source.fromString(value).getLines().toSeq
      case _ =>
        throw new IllegalArgumentException(s"Unsupported data type '$data' (${Option(data).map(_.getClass.getName).orNull})")
    }

    def migrateTo(fieldSet: FieldSet): Data = data match {
      case a: ArrayData => a.copy(fieldSet = fieldSet)
      case b: ByteArrayData => b.copy(fieldSet = fieldSet)
      case j: JsonData => j.copy(fieldSet = fieldSet)
      case t: TextData => t.copy(fieldSet = fieldSet)
      case _ =>
        throw new IllegalArgumentException(s"Unsupported data type '$data' (${Option(data).map(_.getClass.getName).orNull})")
    }

  }

}