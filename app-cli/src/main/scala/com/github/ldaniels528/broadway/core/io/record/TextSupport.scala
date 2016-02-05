package com.github.ldaniels528.broadway.core.io.record

import com.github.ldaniels528.broadway.core.io.Scope

/**
  * Represents text-representation support capability for a record
  */
trait TextSupport {
  self: Record =>

  def fromText(line: String)(implicit scope: Scope): Record

  def toText(implicit scope: Scope): String

}
