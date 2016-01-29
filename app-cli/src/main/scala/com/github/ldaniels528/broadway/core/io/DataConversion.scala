package com.github.ldaniels528.broadway.core.io

import play.api.libs.json._

/**
  * Data Conversion Utility
  */
object DataConversion {

  def convertToJson(aValue: Option[String], `type`: String): JsValue = {
    aValue map { value =>
      `type` match {
        case "boolean" => JsBoolean(value.toLowerCase == "true")
        case "date" => JsNumber(value.toDouble)
        case "double" | "float" | "int" | "long" => JsNumber(value.toDouble)
        case "string" => JsString(value)
        case unknown =>
          throw new IllegalArgumentException(s"Could not convert type '$unknown' to JSON")
      }
    } getOrElse JsNull
  }

}
