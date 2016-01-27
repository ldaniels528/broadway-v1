package com.github.ldaniels528.broadway.core.io.layout.json

import com.github.ldaniels528.broadway.core.io.Data
import com.github.ldaniels528.broadway.core.io.device.{InputSource, OutputSource}
import com.github.ldaniels528.broadway.core.io.layout.{FieldSet, Layout}
import com.github.ldaniels528.broadway.core.scope.Scope

/**
  * MongoDB Layout
  */
case class MongoDbLayout(id: String, fieldSet: FieldSet) extends Layout {

  override def in(scope: Scope, device: InputSource, data: Option[Data]) = data.toList

  override def out(scope: Scope, device: OutputSource, dataSet: Seq[Data], isEOF: Boolean) = dataSet

}