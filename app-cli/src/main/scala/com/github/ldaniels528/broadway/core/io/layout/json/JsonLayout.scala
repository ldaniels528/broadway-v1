package com.github.ldaniels528.broadway.core.io.layout.json

import com.github.ldaniels528.broadway.core.io.Data
import com.github.ldaniels528.broadway.core.io.device.{InputSource, OutputSource}
import com.github.ldaniels528.broadway.core.io.layout.{FieldSet, Layout}
import com.github.ldaniels528.broadway.core.scope.Scope

/**
  * Json Layout
  */
case class JsonLayout(id: String, fieldSets: Seq[FieldSet]) extends Layout {

  override def in(scope: Scope, device: InputSource, data: Option[Data]) = {
    data.map(_.asText).map(text => fieldSets.map(_.decode(text))) getOrElse Nil
  }

  override def out(scope: Scope, device: OutputSource, dataSet: Seq[Data], isEOF: Boolean) = {
    dataSet.flatMap(data => fieldSets.map(fs => Data(fs, fs.encode(data.migrateTo(fs)))))
  }

}