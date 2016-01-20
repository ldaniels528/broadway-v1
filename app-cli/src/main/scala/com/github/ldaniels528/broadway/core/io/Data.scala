package com.github.ldaniels528.broadway.core.io

import play.api.libs.json.JsValue

/**
  * Represents a unit of data
  */
trait Data

/**
  * Represents a binary data array
  *
  * @param bytes the given byte array
  */
case class ByteData(bytes: Array[Byte]) extends Data

/**
  * Represents collection of data
  *
  * @param values the given array of string
  */
case class ArrayData(values: Seq[String]) extends Data

/**
  * Represents JSON data
  *
  * @param js the given [[JsValue JSON object]]
  */
case class JsonData(js: JsValue) extends Data

/**
  * Represents text data
  *
  * @param value the given string value
  */
case class TextData(value: String) extends Data

/**
  * Data Companion Object
  *
  * @author lawrence.daniels@gmail.com
  */
object Data {

  def apply(bytes: Array[Byte]): Data = ByteData(bytes)

  def apply(js: JsValue): Data = JsonData(js)

  def apply(value: String): Data = TextData(value)

  def apply(values: Seq[String]): Data = ArrayData(values)

  /**
    * Data Enrichment
    *
    * @param data the given [[Data data]]
    */
  implicit class DataEnrichment(val data: Data) extends AnyVal {

    def asBytes = data match {
      case ArrayData(values) => values.mkString.getBytes
      case ByteData(bytes) => bytes
      case JsonData(js) => js.toString().getBytes
      case TextData(value) => value.getBytes
      case _ =>
        throw new IllegalArgumentException(s"Unsupported data type '$data' (${Option(data).map(_.getClass.getName).orNull})")
    }

  }

}