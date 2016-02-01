package com.github.ldaniels528.broadway.core.io.device

import com.github.ldaniels528.broadway.core.io.Scope
import com.github.ldaniels528.broadway.core.io.device.TextRecordInputSource.TextInput

/**
  * Text Record Input Source
  */
trait TextRecordInputSource extends InputSource {

  def readLine(scope: Scope): Option[TextInput]

}

/**
  * Text Record Input Source Companion Object
  */
object TextRecordInputSource {

  case class TextInput(line: String, offset: Long)

}