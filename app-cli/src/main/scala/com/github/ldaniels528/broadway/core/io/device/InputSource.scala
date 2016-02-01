package com.github.ldaniels528.broadway.core.io.device

import com.github.ldaniels528.broadway.core.io.{Scope, Data}

/**
  * Represents an Input Source
  */
trait InputSource extends DataSource {

  def read(scope: Scope): Option[Data]

}