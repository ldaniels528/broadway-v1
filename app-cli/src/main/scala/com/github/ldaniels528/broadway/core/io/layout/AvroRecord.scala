package com.github.ldaniels528.broadway.core.io.layout


import com.github.ldaniels528.broadway.core.io.Scope
import com.github.ldaniels528.broadway.core.util.AvroConversion._
import org.apache.avro.Schema
import play.api.libs.json.{JsArray, JsObject, Json}

/**
  * Avro Record implementation
  */
case class AvroRecord(id: String, name: String, namespace: String, fields: Seq[Field])
  extends Record with BinaryRecord with JsonCapability {

  private val schema = new Schema.Parser().parse(toSchemaString)

  override def fromBytes(bytes: Array[Byte])(implicit scope: Scope) = {
    fromJson(transcodeAvroBytesToAvroJson(schema, bytes))
  }

  override def toBytes(implicit scope: Scope) = {
    transcodeJsonToAvroBytes(toJson.toString(), schema)
  }

  /**
    * Generates the Avro Schema
    */
  def toSchemaString: String = {
    Json.obj(
      "type" -> "record",
      "name" -> name,
      "namespace" -> namespace,
      "doc" -> "auto-generated comment",
      "fields" -> JsArray(fields.foldLeft[List[JsObject]](Nil) { (list, field) =>
        Json.obj(
          "name" -> field.name,
          "type" -> field.`type`.toTypeName,
          "doc" -> "auto-generated comment") :: list
      })) toString()
  }

}
