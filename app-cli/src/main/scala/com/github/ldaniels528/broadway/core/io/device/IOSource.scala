package com.github.ldaniels528.broadway.core.io.device

import com.github.ldaniels528.broadway.core.scope.Scope

/**
  * Represents a Generic Input or Output Source
  */
trait IOSource extends StatisticsGeneration {

  def id: String

  def close(scope: Scope): Unit

  def open(scope: Scope): Unit

}
