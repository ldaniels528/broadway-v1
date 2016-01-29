package com.github.ldaniels528.broadway.core.io.device

import com.github.ldaniels528.broadway.core.io.layout.Layout
import com.github.ldaniels528.broadway.core.scope.Scope

/**
  * Represents a Generic Input or Output Source
  */
trait IOSource extends StatisticsGeneration {

  def id: String

  def close(scope: Scope): Unit

  def layout: Layout

  def open(scope: Scope): Unit

}

/**
  * I/O Source Companion Object
  */
object IOSource {

  implicit class IOSourceEnrichment[T <: IOSource](val source: T) extends AnyVal {

    def use[S](block: T => S)(implicit scope: Scope): S = {
      source.open(scope)
      try block(source) finally source.close(scope)
    }

  }

}
