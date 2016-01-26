package com.github.ldaniels528.broadway.core.scope

/**
  * Represents an immutable value
  */
trait Value {

  def eval(scope: Scope): Option[Any]

}
