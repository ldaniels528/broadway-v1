package com.github.ldaniels528.broadway.cli.io.layout.text.fields

import com.github.ldaniels528.broadway.cli.io.layout.{Field, FieldSet}
import com.github.ldaniels528.broadway.cli.io.{ArrayData, Data, TextData, UnsupportedDataTypeException}

import scala.language.postfixOps

/**
  * Fixed Length Field Set
  */
case class FixedLengthFieldSet(fields: Seq[Field]) extends FieldSet {

  override def decode(text: String) = Data(fromFixed(text))

  override def encode(data: Data) = {
    data match {
      case ArrayData(values) => toFixed(values)
      case TextData(value) => toFixed(Seq(value))
      case _ =>
        throw new UnsupportedDataTypeException(data)
    }
  }

  private def extract(text: String, start: Int, end: Int) = {
    if (start > text.length) " " * (end - start)
    else if (end > text.length) text + " " * ((end - start) - text.length)
    else text.substring(start, end)
  }

  private def fromFixed(text: String) = {
    var pos = 0
    fields map { field =>
      val length = field.length getOrElse 1
      val s = extract(text, pos, pos + length)
      pos += length
      s
    }
  }

  private def toFixed(values: Seq[String]) = {
    fields zip values map { case (field, value) =>
      field.length match {
        case Some(size) =>
          if (value.length < size) value + (" " * (size - value.length)) else value.take(size)
        case None => value
      }
    } mkString
  }

}
