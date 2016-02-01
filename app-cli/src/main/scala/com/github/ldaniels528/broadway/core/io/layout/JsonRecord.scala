package com.github.ldaniels528.broadway.core.io.layout

import com.github.ldaniels528.broadway.core.io.Scope

/**
  * Json Record implementation
  */
case class JsonRecord(id: String, fields: Seq[Field]) extends TextRecord with JsonCapability {

  override def fromLine(line: String)(implicit scope: Scope) = fromJson(line)

  override def toLine(implicit scope: Scope) = toJson.toString()

}

