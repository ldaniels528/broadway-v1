package com.github.ldaniels528.broadway.core.io.record

import com.github.ldaniels528.broadway.core.io.Scope

/**
  * Represents binary-representation support capability for a record
  * @author lawrence.daniels@gmail.com
  */
trait BinarySupport {
  self: Record =>

  def fromBytes(bytes: Array[Byte])(implicit scope: Scope): Record

  def toBytes(implicit scope: Scope): Array[Byte]

}
