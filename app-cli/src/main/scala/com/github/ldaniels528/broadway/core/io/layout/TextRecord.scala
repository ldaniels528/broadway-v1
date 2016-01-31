package com.github.ldaniels528.broadway.core.io.layout

/**
  * Represents a text-based record
  */
trait TextRecord extends Record {

  def fromLine(line: String): this.type

  def toLine: String

}
