package com.github.ldaniels528.broadway.cli.io.layout

import com.github.ldaniels528.broadway.cli.io.Data

/**
  * Represents an input layout for incoming data
  */
trait InputLayout extends Layout {

  def decode(offset: Long, text: String): Option[Data]

}