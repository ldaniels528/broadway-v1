package com.github.ldaniels528.broadway.core.io.device

import com.github.ldaniels528.broadway.core.io.{Scope, Data}

/**
  * Represents an Output Source
  */
trait OutputSource extends DataSource {

  def write(scope: Scope, data: Data): Int

}
