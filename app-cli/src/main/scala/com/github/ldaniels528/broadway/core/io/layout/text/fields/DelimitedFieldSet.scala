package com.github.ldaniels528.broadway.core.io.layout.text.fields

import com.github.ldaniels528.broadway.core.io.layout.{FieldSet, Field}
import com.github.ldaniels528.broadway.core.io.{ArrayData, Data, TextData}

/**
  * Delimited Field Set
  */
case class DelimitedFieldSet(fields: Seq[Field], delimiter: String, isQuoted: Boolean = false) extends FieldSet {
  private val splitter = s"[$delimiter]"

  override def decode(text: String) = Data(text.toString.split(splitter).toList)

  override def encode(data: Data) = {
    data match {
      case ArrayData(values) => values.mkString(delimiter)
      case TextData(value) => value
      case _ =>
        throw new IllegalArgumentException(s"Unsupported data type '$data' (${Option(data).map(_.getClass.getName).orNull})")
    }
  }

}
