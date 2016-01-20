package com.github.ldaniels528.broadway.core

import com.github.ldaniels528.broadway.core.scope.GlobalScope

/**
  * ETL Runtime Context
  */
case class RuntimeContext(config: ETLConfig) {
  private val scope = GlobalScope()

  def id = config.id

  def devices = config.flows.flatMap(_.devices).distinct

  def flows = config.flows

  def evaluate(expression: String) = ETLCompiler.handlebars(this, expression)

}
