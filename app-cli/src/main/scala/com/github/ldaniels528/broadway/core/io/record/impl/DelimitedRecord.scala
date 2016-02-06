package com.github.ldaniels528.broadway.core.io.record.impl

import com.github.ldaniels528.broadway.core.io.Scope
import com.github.ldaniels528.broadway.core.io.record.Field._
import com.github.ldaniels528.broadway.core.io.record.{Field, Record, TextSupport}

/**
  * Delimited Record
  * @author lawrence.daniels@gmail.com
  */
case class DelimitedRecord(id: String,
                           delimiter: Char,
                           isTextQuoted: Boolean = false,
                           isNumbersQuoted: Boolean = false,
                           fields: Seq[Field])
  extends Record with TextSupport {

  override def fromText(line: String)(implicit scope: Scope) = {
    val values = fromDelimitedText(line)
    fields zip values foreach { case (field, value) =>
      field.value = value.convert(field.`type`)
    }
    this
  }

  override def toText(implicit scope: Scope) = toDelimitedText(fields.map(_.value.getOrElse("")))

  private def fromDelimitedText(text: String) = {
    var inQuotes = false
    val sb = new StringBuilder()
    val values = text.toCharArray.foldLeft[List[String]](Nil) {
      case (list, '"') =>
        inQuotes = !inQuotes
        list
      case (list, ch) if ch == delimiter =>
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

  private def toDelimitedText(values: Seq[Any]) = {
    val text = values.foldLeft(new StringBuilder()) { (sb, value) =>
      sb.append(delimiter).append(quote(value))
    }
    text.toString().tail
  }

  private def quote(value: Any) = value match {
    case n@(_: BigDecimal | _: BigInt | _: Byte | _: Double | _: Float | _: Int | _: Long | _: Number) if !isNumbersQuoted => n.toString
    case s: String if isTextQuoted => s""""$s""""
    case x => x.toString
  }

}
