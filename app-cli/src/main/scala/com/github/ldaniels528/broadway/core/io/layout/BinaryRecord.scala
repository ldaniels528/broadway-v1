package com.github.ldaniels528.broadway.core.io.layout

/**
  * Represents a Binary Record
  */
trait BinaryRecord extends Record {

  def fromBytes(bytes: Array[Byte]): Unit

  def toBytes: Array[Byte]

}
