package com.github.ldaniels528.broadway.core.io.layout

import com.github.ldaniels528.broadway.core.io.Scope

/**
  * Represents a text-based record
  */
trait TextRecord extends Record {

  def fromLine(line: String)(implicit scope: Scope): this.type

  def toLine(implicit scope: Scope): String

}
