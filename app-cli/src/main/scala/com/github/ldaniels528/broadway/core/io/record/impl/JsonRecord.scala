package com.github.ldaniels528.broadway.core.io.record.impl

import com.github.ldaniels528.broadway.core.io.Scope
import com.github.ldaniels528.broadway.core.io.device.DataSet
import com.github.ldaniels528.broadway.core.io.record.{Field, JsonSupport, Record, TextSupport}

/**
  * Json Record implementation
  * @author lawrence.daniels@gmail.com
  */
case class JsonRecord(id: String, fields: Seq[Field]) extends Record with JsonSupport with TextSupport {

  override def fromText(jsonString: String)(implicit scope: Scope) = fromJson(jsonString)

  override def toText(dataSet: DataSet)(implicit scope: Scope) = toJson(dataSet).toString()

}

