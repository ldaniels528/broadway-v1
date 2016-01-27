package com.github.ldaniels528.broadway.core.scope

import com.github.ldaniels528.broadway.core.scope.Scope._

import scala.collection.concurrent.TrieMap

/**
  * Default Scope
  */
class DefaultScope() extends Scope {
  private val variables = TrieMap[String, Any]()

  override def ++=(values: Seq[(String, Any)]) = variables ++= values

  override def +=(tuple: (String, Any)) = variables += tuple

  override def find(name: String) = {
    variables.get(name) map {
      case fx: DynamicValue => fx()
      case value => value
    }
  }

  override def getOrElseUpdate[T](key: String, value: => T) = {
    variables.getOrElseUpdate(key, value).asInstanceOf[T]
  }

  override def putIfAbsent(values: Seq[(String, Any)]) = {
    values foreach { case (name, value) =>
      variables.putIfAbsent(name, value)
    }
  }

  ///////////////////////////////////////////////////////////////////////
  //    IOSource-specific Methods
  ///////////////////////////////////////////////////////////////////////

  override def createResource[T](id: String, action: => T) = variables.getOrElseUpdate(id, action).asInstanceOf[T]

  override def discardResource[T](id: String) = variables.remove(id).map(_.asInstanceOf[T])

  override def getResource[T](id: String) = variables.get(id).map(_.asInstanceOf[T])

}

