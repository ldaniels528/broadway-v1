package com.github.ldaniels528.broadway.cli.io.layout.text.fields

import com.github.ldaniels528.broadway.cli.io._
import com.github.ldaniels528.broadway.cli.io.layout.text.fields.JsonFieldSet.toJsonText
import com.github.ldaniels528.broadway.cli.io.layout.{Field, FieldSet}
import play.api.libs.json.Json

/**
  * JSON Field Set
  */
case class JsonFieldSet(fields: Seq[Field]) extends FieldSet {

  override def decode(text: String) = Data(Json.parse(text))

  override def encode(data: Data) = {
    data match {
      case ArrayData(values) => toJsonText(fields.map(_.name) zip values)
      case JsonData(js) => js.toString()
      case TextData(value) => toJsonText(fields.map(_.name) zip Seq(value))
      case _ =>
        throw new UnsupportedDataTypeException(data)
    }
  }

}

/**
  * JSON Field Set Companion Object
  */
object JsonFieldSet {

  def toJsonText(values: Seq[(String, String)]) = {
    values.foldLeft(Json.obj()) { case (js, (k, v)) => js ++ Json.obj(k -> v) }.toString()
  }

}