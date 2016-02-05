package com.github.ldaniels528.broadway.core.io.record.impl

import com.github.ldaniels528.broadway.core.io.Scope
import com.github.ldaniels528.broadway.core.io.record.Field._
import com.github.ldaniels528.broadway.core.io.record.{Field, Record, TextSupport}

/**
  * Comma Separated Values (CSV) Record implementation
  * @author lawrence.daniels@gmail.com
  */
case class CsvRecord(id: String, fields: Seq[Field]) extends Record with TextSupport {

  override def fromText(line: String)(implicit scope: Scope) = {
    val values = fromCSV(line)
    fields zip values foreach { case (field, value) =>
      field.value = value.convert(field.`type`)
    }
    this
  }

  override def toText(implicit scope: Scope) = toCSV(fields.map(_.value.getOrElse("")))

  private def fromCSV(text: String) = {
    var inQuotes = false
    val sb = new StringBuilder()
    val values = text.toCharArray.foldLeft[List[String]](Nil) {
      case (list, '"') =>
        inQuotes = !inQuotes
        list
      case (list, ch) if ch == ',' =>
        if (inQuotes) {
          sb.append(ch)
          list
        }
        else {
          val s = sb.toString().trim
          sb.clear()
          s :: list
        }
      case (list, ch) =>
        sb.append(ch)
        list
    }
    (if (sb.nonEmpty) sb.toString().trim :: values else values).reverse
  }

  private def toCSV(values: Seq[Any]) = {
    val text = values.foldLeft(new StringBuilder()) { (sb, value) =>
      sb.append(s""",${quote(value)}""")
    }
    text.toString().tail
  }

  private def quote(value: Any) = value match {
    case n@(_: BigDecimal | _: BigInt | _: Byte | _: Double | _: Float | _: Int | _: Long | _: Number) => n.toString
    case s => s""""$s""""
  }

}
