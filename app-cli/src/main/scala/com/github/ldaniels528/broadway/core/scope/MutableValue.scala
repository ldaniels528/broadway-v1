package com.github.ldaniels528.broadway.core.scope

/**
  * Represents a mutable value
  */
trait MutableValue extends Value {

  def set_=(value: Any): Unit

}
