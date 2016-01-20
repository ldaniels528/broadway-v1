package com.github.ldaniels528.broadway.core.flow

import com.github.ldaniels528.broadway.core.RuntimeContext

/**
  * Step Through Flow
  */
trait StepThroughFlow extends Flow {

  def firstStep(rt: RuntimeContext): Unit

  def nextStep(rt: RuntimeContext): Boolean

  def lastStep(rt: RuntimeContext): Unit

}
