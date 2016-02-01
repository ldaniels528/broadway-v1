package com.github.ldaniels528.broadway.core.io.device

import com.github.ldaniels528.broadway.core.io.Scope
import com.github.ldaniels528.broadway.core.io.layout.Record

/**
  * Record Output Source
  */
trait RecordOutputSource extends OutputSource {

  def writeRecord(record: Record)(implicit scope: Scope): Int

}
