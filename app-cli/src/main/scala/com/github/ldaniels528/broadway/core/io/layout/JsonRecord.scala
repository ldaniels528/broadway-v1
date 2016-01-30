package com.github.ldaniels528.broadway.core.io.layout

import com.github.ldaniels528.broadway.core.io.layout.Record.Element
import com.github.ldaniels528.broadway.core.io.layout.RecordTypes._

/**
  * Json Record implementation
  */
case class JsonRecord(fields: Seq[Element], `type`: RecordType) extends TextRecord with JsonCapability {

  override def fromLine(line: String) = fromJson(line)

  override def toLine = toJson.toString()

}

