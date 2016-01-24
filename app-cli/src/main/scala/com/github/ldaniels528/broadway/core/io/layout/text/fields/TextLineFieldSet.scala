package com.github.ldaniels528.broadway.core.io.layout.text.fields

import com.github.ldaniels528.broadway.core.io.Data
import com.github.ldaniels528.broadway.core.io.layout.{Field, FieldSet}

/**
  * Text Line Field Set
  */
case class TextLineFieldSet(text: String) extends FieldSet {
  val fields: Seq[Field] = Seq(Field(text))

  override def decode(text: String) = Data(this, text)

  override def encode(data: Data) = text

}
