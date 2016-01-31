package com.github.ldaniels528.broadway.core.io.device

import com.github.ldaniels528.broadway.core.io.layout.{TextRecord, Record}
import com.github.ldaniels528.broadway.core.scope.Scope

/**
  * Text Record Output Source
  */
trait TextRecordOutputSource extends OutputSource {

  def writeRecord(record: Record)(implicit scope: Scope): Int

  def templateRecord: TextRecord

}
