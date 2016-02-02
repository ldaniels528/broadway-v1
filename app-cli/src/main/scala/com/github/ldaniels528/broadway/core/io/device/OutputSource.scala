package com.github.ldaniels528.broadway.core.io.device

import com.github.ldaniels528.broadway.core.io.Scope
import com.github.ldaniels528.broadway.core.io.layout.Record

/**
  * Represents an Output Source
  */
trait OutputSource extends DataSource {

  def writeRecord(record: Record)(implicit scope: Scope): Int

}
