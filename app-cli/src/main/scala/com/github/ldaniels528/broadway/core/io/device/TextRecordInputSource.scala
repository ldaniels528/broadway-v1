package com.github.ldaniels528.broadway.core.io.device

import com.github.ldaniels528.broadway.core.io.layout.{TextRecord, Record}
import com.github.ldaniels528.broadway.core.scope.Scope

/**
  * Text Record Input Source
  */
trait TextRecordInputSource extends InputSource {

  def readRecord(implicit scope: Scope): Option[Record]

  def templateRecord: TextRecord

}
