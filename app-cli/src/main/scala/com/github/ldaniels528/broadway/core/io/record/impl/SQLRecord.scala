package com.github.ldaniels528.broadway.core.io.record.impl

import com.github.ldaniels528.broadway.core.io.record.{Record, Field}

/**
  * Represents a SQL Record
  * @author lawrence.daniels@gmail.com
  */
case class SQLRecord(id: String, fields: Seq[Field]) extends Record {

  def containsCondition = fields.exists(_.updateKey.contains(true))

}
