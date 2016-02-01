package com.github.ldaniels528.broadway.core.io.layout

import com.github.ldaniels528.broadway.core.io.Scope
import com.github.ldaniels528.broadway.core.io.layout.Field._

/**
  * Delimited Record
  */
case class DelimitedRecord(id: String, delimiter: String, fields: Seq[Field]) extends TextRecord {
  private val splitter = s"[$delimiter]"

  override def fromLine(line: String)(implicit scope: Scope) = {
    fields zip line.split(splitter) foreach { case (field, value) =>
      field.value = value.convert(field.`type`)
    }
    this
  }

  override def toLine(implicit scope: Scope) = fields.map(_.value.getOrElse("")).mkString(delimiter)

}
