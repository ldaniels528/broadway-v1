package com.github.ldaniels528.broadway.core.io.record

import com.github.ldaniels528.broadway.core.io.Scope

/**
  * Represents text-representation support capability for a record
  */
trait TextSupport {
  self: Record =>

  def fromLine(line: String)(implicit scope: Scope): Record

  def toLine(implicit scope: Scope): String

}
