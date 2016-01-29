package com.github.ldaniels528.broadway.core.io

import com.github.ldaniels528.broadway.core.scope.Scope

import scala.concurrent.ExecutionContext

/**
  * Represents an executable opCode
  */
trait OpCode[T] {

  def execute(implicit scope: Scope, ec: ExecutionContext): T

}
