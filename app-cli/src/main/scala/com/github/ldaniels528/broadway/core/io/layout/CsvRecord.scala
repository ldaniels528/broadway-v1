package com.github.ldaniels528.broadway.core.io.layout


import com.github.ldaniels528.broadway.core.io.layout.RecordTypes.RecordType
import com.ldaniels528.commons.helpers.OptionHelper.Risky._

/**
  * Comma Separated Values (CSV) Record implementation
  */
case class CsvRecord(id: String, fields: Seq[Field], `type`: RecordType) extends TextRecord {

  override def duplicate = this.copy()

  override def fromLine(line: String) = {
    val values = fromCSV(line)
    fields zip values foreach { case (field, value) =>
      field.value = value
    }
    this
  }

  override def toLine = toCSV(fields.map(_.value.map(_.toString).getOrElse("")))

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

  private def toCSV(values: Seq[String]) = {
    val text = values.foldLeft(new StringBuilder())((sb, value) => sb.append(s""","$value""""))
    text.toString().tail
  }

}
