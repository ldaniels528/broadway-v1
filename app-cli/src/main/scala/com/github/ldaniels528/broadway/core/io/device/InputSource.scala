package com.github.ldaniels528.broadway.core.io.device

import com.github.ldaniels528.broadway.core.io.Scope
import com.github.ldaniels528.broadway.core.io.record.Record

/**
  * Represents an Input Source
  * @author lawrence.daniels@gmail.com
  */
trait InputSource extends DataSource {

  def read(record: Record)(implicit scope: Scope): Option[DataSet]

}
