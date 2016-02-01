package com.github.ldaniels528.broadway.core.io.layout

import com.github.ldaniels528.broadway.core.io.Scope
import com.github.ldaniels528.broadway.core.io.layout.Field._

import scala.language.postfixOps

/**
  * Fixed Length Record implementation
  */
case class FixedLengthRecord(id: String, fields: Seq[Field]) extends TextRecord {

  override def fromLine(line: String)(implicit scope: Scope) = {
    var pos = 0
    fields foreach { field =>
      val length = field.length getOrElse 1
      field.value = extract(line, pos, pos + length).trim.convert(field.`type`)
      pos += length
    }
    this
  }

  override def toLine(implicit scope: Scope) = {
    fields.foldLeft[StringBuilder](new StringBuilder) { (sb, field) =>
      val raw = field.value.map(_.toString).getOrElse("")
      val length = field.length.getOrElse(raw.length)
      val sized = if (raw.length > length) raw.take(length) else raw + " " * (length - raw.length)
      sb.append(sized)
    } toString()
  }

  private def extract(text: String, start: Int, end: Int) = {
    if (start > text.length) " " * (end - start)
    else if (end > text.length) text.substring(start) + " " * ((end - start) - text.length)
    else text.substring(start, end)
  }

}
