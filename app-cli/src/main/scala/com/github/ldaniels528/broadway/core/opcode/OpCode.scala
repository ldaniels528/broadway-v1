package com.github.ldaniels528.broadway.core.opcode

import com.github.ldaniels528.broadway.core.RuntimeContext

/**
  * Represents an executable opCode
  */
trait OpCode {

  def eval(rt: RuntimeContext): Option[Any]

}
