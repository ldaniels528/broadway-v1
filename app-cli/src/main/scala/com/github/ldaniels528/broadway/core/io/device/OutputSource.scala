package com.github.ldaniels528.broadway.core.io.device

import com.github.ldaniels528.broadway.core.io.Scope
import com.github.ldaniels528.broadway.core.io.record.Record

/**
  * Represents an Output Source
  * @author lawrence.daniels@gmail.com
  */
trait OutputSource extends DataSource {

  def writeRecord(record: Record, dataSet: DataSet)(implicit scope: Scope): Int

}
