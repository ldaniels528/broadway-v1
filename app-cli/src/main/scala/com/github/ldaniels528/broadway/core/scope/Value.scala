package com.github.ldaniels528.broadway.core.scope

import com.github.ldaniels528.broadway.core.RuntimeContext

/**
  * Represents an immutable value
  */
trait Value {

  def eval(rt: RuntimeContext): Option[Any]

}
