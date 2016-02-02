package com.github.ldaniels528.broadway.core.io.device

import com.github.ldaniels528.broadway.core.io.Scope
import com.github.ldaniels528.broadway.core.io.device.TextReadingSupport.TextInput

/**
  * Text Record Input Source
  * @author lawrence.daniels@gmail.com
  */
trait TextReadingSupport {
  self: InputSource =>

  def readLine(implicit scope: Scope): Option[TextInput]

}

/**
  * Text Record Input Source Companion Object
  * @author lawrence.daniels@gmail.com
  */
object TextReadingSupport {

  case class TextInput(line: String, offset: Long)

}