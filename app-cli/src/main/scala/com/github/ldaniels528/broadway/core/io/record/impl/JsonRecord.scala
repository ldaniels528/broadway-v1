package com.github.ldaniels528.broadway.core.io.record.impl

import com.github.ldaniels528.broadway.core.io.Scope
import com.github.ldaniels528.broadway.core.io.record.{Field, JsonSupport, Record, TextSupport}

/**
  * Json Record implementation
  * @author lawrence.daniels@gmail.com
  */
case class JsonRecord(id: String, fields: Seq[Field]) extends Record with TextSupport with JsonSupport {

  override def fromLine(line: String)(implicit scope: Scope) = fromJson(line)

  override def toLine(implicit scope: Scope) = toJson.toString()

}

