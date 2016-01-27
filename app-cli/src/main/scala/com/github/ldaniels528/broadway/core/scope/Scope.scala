package com.github.ldaniels528.broadway.core.scope

import com.github.ldaniels528.broadway.core.compiler.ExpressionCompiler

/**
  * Represents a runtime scope
  */
trait Scope {

  def +=(tuple: (String, Any)): Unit

  def ++=(values: Seq[(String, Any)]): Unit

  def evaluate(expression: String) = ExpressionCompiler.handlebars(this, expression)

  def find(name: String): Option[Any]

  def getOrElseUpdate[T](key: String, value: => T): T

  def putIfAbsent(values: Seq[(String, Any)]): Unit

  ///////////////////////////////////////////////////////////////////////
  //    Resource-specific Methods
  ///////////////////////////////////////////////////////////////////////

  def createResource[T](id: String, action: => T): T

  def discardResource[T](id: String): Option[T]

  def getResource[T](id: String): Option[T]

}

/**
  * Scope Companion Object
  */
object Scope {

  type DynamicValue = () => Any

}