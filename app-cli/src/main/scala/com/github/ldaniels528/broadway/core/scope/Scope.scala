package com.github.ldaniels528.broadway.core.scope

import com.github.ldaniels528.broadway.core.ETLCompiler
import com.github.ldaniels528.broadway.core.scope.Scope.ScopeFunction

/**
  * Represents a runtime scope
  */
trait Scope {

  def add(name: String, value: Any): Unit

  def find(name: String, property: String): Option[ScopeFunction]

  def evaluate(expression: String) = ETLCompiler.handlebars(this, expression)

  def getReader[T]: T

  def getWriter[T]: T

  def openReader[T](action: => T): T

  def openWriter[T](action: => T): T

}

/**
  * Scope Companion Object
  */
object Scope {

  type ScopeFunction = Scope => Any

}