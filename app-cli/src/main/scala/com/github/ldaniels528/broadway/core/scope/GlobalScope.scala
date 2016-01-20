package com.github.ldaniels528.broadway.core.scope

import scala.collection.concurrent.TrieMap

/**
  * Global Scope
  */
case class GlobalScope() extends Scope {
  private val namedValues = TrieMap[String, Value]()

  def find(id: String) = namedValues.get(id)

}

