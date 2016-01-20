package com.github.ldaniels528.broadway.core.io.layout

import com.github.ldaniels528.broadway.core.io.Data

/**
  * Represents an output layout for outgoing data
  */
trait OutputLayout extends Layout {

  def encode(offset: Long, data: Data): Option[String]

}