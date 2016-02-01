package com.github.ldaniels528.broadway.core.io.layout

import com.github.ldaniels528.broadway.core.io.Scope

/**
  * Represents a Binary Record
  */
trait BinaryRecord extends Record {

  def fromBytes(bytes: Array[Byte])(implicit scope: Scope): Unit

  def toBytes(implicit scope: Scope): Array[Byte]

}
