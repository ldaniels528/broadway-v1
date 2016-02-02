package com.github.ldaniels528.broadway.core.io

import com.github.ldaniels528.broadway.core.compiler.ExpressionCompiler
import com.github.ldaniels528.broadway.core.io.Scope.DynamicValue

import scala.collection.concurrent.TrieMap

/**
  * Represents a runtime scope
  * @author lawrence.daniels@gmail.com
  */
class Scope(values: (String, Any)*) {
  private val variables = TrieMap[String, Any](values: _*)

  def ++=(values: Seq[(String, Any)]) = variables ++= values

  def +=(tuple: (String, Any)) = variables += tuple

  def evaluate(expression: String) = ExpressionCompiler.handlebars(this, expression)

  def find(name: String): Option[Any] = {
    variables.get(name) map {
      case fx: DynamicValue => fx()
      case value => value
    }
  }

  def getOrElseUpdate[T](key: String, value: => T): T = {
    variables.getOrElseUpdate(key, value).asInstanceOf[T]
  }

  def toSeq = variables.toSeq

  ///////////////////////////////////////////////////////////////////////
  //    Resource-specific Methods
  ///////////////////////////////////////////////////////////////////////

  def createResource[T](id: String, action: => T) = variables.getOrElseUpdate(id, action).asInstanceOf[T]

  def discardResource[T](id: String) = variables.remove(id).map(_.asInstanceOf[T])

  def getResource[T](id: String) = variables.get(id).map(_.asInstanceOf[T])

}

/**
  * Scope Companion Object
  * @author lawrence.daniels@gmail.com
  */
object Scope {

  type DynamicValue = () => Any

  def apply() = new Scope()

  def apply(rootScope: Scope) = new Scope(rootScope.toSeq: _*)

}