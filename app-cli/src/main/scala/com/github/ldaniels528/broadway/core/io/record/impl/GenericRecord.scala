package com.github.ldaniels528.broadway.core.io.record.impl

import com.github.ldaniels528.broadway.core.io.record.{Record, Field}

/**
  * Represents a generic record implementation
  * @author lawrence.daniels@gmail.com
  */
case class GenericRecord(id: String, fields: Seq[Field]) extends Record