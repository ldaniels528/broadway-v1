package com.github.ldaniels528.broadway.core.io.record

import com.github.ldaniels528.broadway.core.io.Scope
import play.api.libs.json.JsObject

/**
  * Represents a generic data record
  */
trait Record {

  def id: String

  def fields: Seq[Field]

  def toFullString(implicit scope: Scope) = s"${getClass.getSimpleName}(${fields.map(f => s"${f.name}=${f.value}").mkString(", ")})"

  override def toString = s"${getClass.getSimpleName}(${fields.map(f => s"${f.name}=...").mkString(", ")})"

}

/**
  * Record Companion Object
  */
object Record {

  /**
    * Record Enrichment Utilities
    * @param record the given [[Record record]]
    */
  implicit class RecordEnrichment[T <: Record](val record: T) extends AnyVal {

    def convertToBinary(implicit scope: Scope): Array[Byte] = record match {
      case rec: BinarySupport => rec.toBytes
      case rec: JsonSupport => rec.toJson.toString().getBytes
      case rec: TextSupport => rec.toText.getBytes()
      case rec => throw new UnsupportedRecordTypeException(rec)
    }

    def convertToJson(implicit scope: Scope): JsObject = record match {
      case rec: BinarySupport => JsonSupport.parse(new String(rec.toBytes))
      case rec: JsonSupport => rec.toJson
      case rec: TextSupport => JsonSupport.parse(rec.toText)
      case rec => throw new UnsupportedRecordTypeException(rec)
    }

    def convertToText(implicit scope: Scope): String = record match {
      case rec: TextSupport => rec.toText
      case rec: JsonSupport => rec.toJson.toString()
      case rec: BinarySupport => new String(rec.toBytes)
      case rec => throw new UnsupportedRecordTypeException(rec)
    }

    def importText(text: String)(implicit scope: Scope) = {
      record match {
        case rec: TextSupport => rec.fromText(text)
        case rec => throw new UnsupportedRecordTypeException(rec)
      }
      record
    }

  }

}