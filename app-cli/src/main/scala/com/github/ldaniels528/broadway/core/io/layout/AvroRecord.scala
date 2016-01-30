package com.github.ldaniels528.broadway.core.io.layout

import com.github.ldaniels528.broadway.core.io.layout.Record.Element
import com.github.ldaniels528.broadway.core.io.layout.RecordTypes._
import com.github.ldaniels528.broadway.core.util.AvroConversion._
import org.apache.avro.Schema
import play.api.libs.json.{JsArray, JsObject, Json}

/**
  * Avro Record implementation
  */
case class AvroRecord(id: String, namespace: String, fields: Seq[Element], `type`: RecordType)
  extends Record with BinaryRecord with JsonCapability {

  private val schema = new Schema.Parser().parse(toSchemaString)

  override def fromBytes(bytes: Array[Byte]) = fromJson(transcodeAvroBytesToAvroJson(schema, bytes))

  override def toBytes = transcodeJsonToAvroBytes(toJson.toString(), schema)

  /**
    * Generates the Avro Schema
    */
  def toSchemaString: String = {
    Json.obj(
      "type" -> "record",
      "name" -> id,
      "namespace" -> namespace,
      "doc" -> "auto-generated comment",
      "fields" -> JsArray(fields.foldLeft[List[JsObject]](Nil) { (list, field) =>
        Json.obj(
          "name" -> field.name,
          "type" -> field.`type`.toTypeName,
          "doc" -> "auto-generated comment") :: list
      })) toString()
  }

  override def toString = s"${getClass.getSimpleName}($toJson)"

}
