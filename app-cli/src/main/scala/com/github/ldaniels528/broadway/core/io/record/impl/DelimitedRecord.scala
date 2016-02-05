package com.github.ldaniels528.broadway.core.io.record.impl

import com.github.ldaniels528.broadway.core.io.Scope
import com.github.ldaniels528.broadway.core.io.record.Field._
import com.github.ldaniels528.broadway.core.io.record.{Field, Record, TextSupport}

/**
  * Delimited Record
  * @author lawrence.daniels@gmail.com
  */
case class DelimitedRecord(id: String, delimiter: String, fields: Seq[Field]) extends Record with TextSupport {
  private val splitter = s"[$delimiter]"

  override def fromText(line: String)(implicit scope: Scope) = {
    fields zip line.split(splitter) foreach { case (field, value) =>
      field.value = value.convert(field.`type`)
    }
    this
  }

  override def toText(implicit scope: Scope) = fields.map(_.value.getOrElse("")).mkString(delimiter)

}
