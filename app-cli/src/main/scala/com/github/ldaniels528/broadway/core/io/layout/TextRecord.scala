package com.github.ldaniels528.broadway.core.io.layout

/**
  * Represents a text-based record
  */
trait TextRecord extends Record {

  def fromLine(line: String): Unit

  def toLine: String

  override def toString = s"${getClass.getSimpleName}($toLine)"

}
