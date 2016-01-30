package com.github.ldaniels528.broadway.core.io.layout.json

import com.github.ldaniels528.broadway.core.io._
import com.github.ldaniels528.broadway.core.io.layout.{Field, FieldSet}
import play.api.libs.json.Json

/**
  * JSON Field Set
  */
case class JsonFieldSet(fields: Seq[Field]) extends FieldSet {

  override def decode(text: String) = Data(this, Json.parse(text))

  override def encode(data: Data) = data.asJson.toString()

}
