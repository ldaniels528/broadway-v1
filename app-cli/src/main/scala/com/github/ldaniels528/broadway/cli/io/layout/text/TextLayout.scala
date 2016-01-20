package com.github.ldaniels528.broadway.cli.io.layout.text

import com.github.ldaniels528.broadway.cli.io.Data
import com.github.ldaniels528.broadway.cli.io.layout._

import scala.language.postfixOps

/**
  * Text Layout
  */
case class TextLayout(id: String, fields: FieldSet, header: Option[Header] = None, footer: Option[Footer] = None)
  extends InputLayout with OutputLayout {

  override def decode(offset: Long, text: String) = {
    if (header.exists(_.length >= offset)) None else Option(fields.decode(text))
  }

  override def encode(offset: Long, data: Data) = {
    Option(fields.encode(data))
  }

}
