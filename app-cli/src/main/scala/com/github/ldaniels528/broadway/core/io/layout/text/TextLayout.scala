package com.github.ldaniels528.broadway.core.io.layout.text

import com.github.ldaniels528.broadway.core.io.Data
import com.github.ldaniels528.broadway.core.io.layout._

import scala.language.postfixOps

/**
  * Text Layout
  */
case class TextLayout(id: String, header: Seq[Division], body: Seq[Division], footer: Seq[Division])
  extends InputLayout with OutputLayout {

  override def decode(offset: Long, text: String) = {
    if (header.length >= offset) None else body.map(_.fieldSet.decode(text)).headOption
  }

  override def encode(offset: Long, data: Data) = {
    body.map(_.fieldSet.encode(data)).headOption
  }

}
