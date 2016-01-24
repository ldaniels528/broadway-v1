package com.github.ldaniels528.broadway.core.io.layout.json

import com.github.ldaniels528.broadway.core.RuntimeContext
import com.github.ldaniels528.broadway.core.io.Data
import com.github.ldaniels528.broadway.core.io.device.{InputDevice, OutputDevice}
import com.github.ldaniels528.broadway.core.io.layout.{FieldSet, Layout}

/**
  * MongoDB Layout
  */
case class MongoDbLayout(id: String, fieldSet: FieldSet) extends Layout {

  override def in(rt: RuntimeContext, device: InputDevice, data: Option[Data]) = data.toList

  override def out(rt: RuntimeContext, device: OutputDevice, dataSet: Seq[Data], isEOF: Boolean) = dataSet

}