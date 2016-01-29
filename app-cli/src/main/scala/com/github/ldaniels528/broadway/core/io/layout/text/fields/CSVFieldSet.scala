package com.github.ldaniels528.broadway.core.io.layout.text.fields

import com.github.ldaniels528.broadway.core.io.Data
import com.github.ldaniels528.broadway.core.io.Data._
import com.github.ldaniels528.broadway.core.io.layout.{Field, FieldSet}

/**
  * Comma Separated Values (CSV) Field Set
  */
case class CSVFieldSet(fields: Seq[Field]) extends FieldSet {

  override def decode(text: String) = Data(this, fromCSV(text))

  override def encode(data: Data) = {
    data match {
      case ArrayData(_, values) => toCSV(values)
      case TextData(_, value) => toCSV(fromCSV(value))
      case _ =>
        throw new IllegalArgumentException(s"Unsupported data type '$data' (${Option(data).map(_.getClass.getName).orNull})")
    }
  }

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
          val s = sb.toString()
          sb.clear()
          s :: list
        }
      case (list, ch) =>
        sb.append(ch)
        list
    }
    (if (sb.nonEmpty) sb.toString() :: values else values).reverse
  }

  private def toCSV(values: Seq[String]) = {
    val text = values.foldLeft(new StringBuilder())((sb, value) => sb.append(s""","$value""""))
    text.toString().tail
  }

}