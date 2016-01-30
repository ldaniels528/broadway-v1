package com.github.ldaniels528.broadway.core.io.layout

import com.github.ldaniels528.broadway.core.io.layout.Record.Element
import com.github.ldaniels528.broadway.core.io.layout.RecordTypes._
import com.ldaniels528.commons.helpers.OptionHelper.Risky._

/**
  * Delimited Record
  */
case class DelimitedRecord(fields: Seq[Element], `type`: RecordType, delimiter: String) extends TextRecord {
  private val splitter = s"[$delimiter]"

  override def fromLine(line: String) = {
    fields zip line.split(splitter) foreach { case (field, value) =>
      field.value = value
    }
  }

  override def toLine = fields.map(_.value.getOrElse("")).mkString(delimiter)

}
