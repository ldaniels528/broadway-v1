package com.github.ldaniels528.broadway.core.io.device

import com.github.ldaniels528.broadway.core.io.Data
import com.github.ldaniels528.broadway.core.scope.Scope

/**
  * Represents an Output Source
  */
trait OutputSource extends IOSource {

  def write(scope: Scope, data: Data): Int

}
