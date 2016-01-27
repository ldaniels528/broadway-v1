package com.github.ldaniels528.broadway.core.io

import com.github.ldaniels528.broadway.core.scope.Scope

/**
  * Represents an executable opCode
  */
trait OpCode[T] {

  def execute(scope: Scope): T

}
