package com.github.ldaniels528.broadway.core.io.layout


import com.github.ldaniels528.broadway.core.io.layout.RecordTypes._
import com.ldaniels528.commons.helpers.OptionHelper.Risky._

/**
  * Delimited Record
  */
case class DelimitedRecord(id: String, fields: Seq[Field], `type`: RecordType, delimiter: String) extends TextRecord {
  private val splitter = s"[$delimiter]"

  override def duplicate = this.copy()

  override def fromLine(line: String) = {
    fields zip line.split(splitter) foreach { case (field, value) =>
      field.value = value
    }
    this
  }

  override def toLine = fields.map(_.value.getOrElse("")).mkString(delimiter)

}
