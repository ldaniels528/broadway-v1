package com.github.ldaniels528.broadway.core.io

import scala.concurrent.ExecutionContext

/**
  * Represents an executable opCode
  */
trait OpCode[T] {

  def execute(scope: Scope)(implicit ec: ExecutionContext): T

}
