package com.github.ldaniels528.broadway.core.io.record.impl

import com.github.ldaniels528.broadway.core.io.Scope
import com.github.ldaniels528.broadway.core.io.record.Field._
import com.github.ldaniels528.broadway.core.io.record.{Field, Record, TextSupport}

import scala.language.postfixOps

/**
  * Fixed Length Record implementation
  * @author lawrence.daniels@gmail.com
  */
case class FixedRecord(id: String, fields: Seq[Field]) extends Record with TextSupport {

  override def fromText(line: String)(implicit scope: Scope) = {
    var pos = 0
    fields foreach { field =>
      val length = field.length getOrElse 1
      field.value = extract(line, pos, pos + length).trim.convert(field.`type`)
      pos += length
    }
    this
  }

  override def toText(implicit scope: Scope) = {
    fields.foldLeft[StringBuilder](new StringBuilder) { (sb, field) =>
      val raw = field.value.map(_.toString).getOrElse("")
      val length = field.length.getOrElse(raw.length)
      val sized = if (raw.length > length) raw.take(length) else raw + " " * (length - raw.length)
      sb.append(sized)
    } toString()
  }

  /**
    * Returns the record length
    * @return the record length
    */
  def length = fields.flatMap(_.length).sum

  /**
    * Extracts a fixed-length portion of the given text
    * @param text the given text
    * @param start the starting position of the substring
    * @param end the ending position of the substring
    * @return a fixed-length portion
    */
  private def extract(text: String, start: Int, end: Int) = {
    if (start > text.length) " " * (end - start)
    else if (end > text.length) text.substring(start) + " " * ((end - start) - text.length)
    else text.substring(start, end)
  }

}
