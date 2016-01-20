package com.github.ldaniels528.broadway.cli.io.layout

import com.github.ldaniels528.broadway.cli.EtlConfig
import com.github.ldaniels528.broadway.cli.io.Data

/**
  * Document Layout Header
  */
case class Header(fields: FieldSet, length: Int) {

  def generate(config: EtlConfig) = Data(fields.fields.map(f => config.evaluate(f.name)))

}