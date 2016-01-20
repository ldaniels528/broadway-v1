package com.github.ldaniels528.broadway.core.io.layout

import com.github.ldaniels528.broadway.core.io.Data

/**
  * Field Set Container
  */
trait FieldSet {

  def fields: Seq[Field]

  def decode(text: String): Data

  def encode(data: Data): String

}
