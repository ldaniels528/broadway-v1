package com.github.ldaniels528.broadway.core.io.layout

import com.github.ldaniels528.broadway.core.io.layout.DataTypes._
import com.github.ldaniels528.broadway.core.io.layout.JsonCapability._
import com.github.ldaniels528.broadway.core.io.layout.Record.Element
import com.ldaniels528.commons.helpers.OptionHelper.Risky._
import com.ldaniels528.commons.helpers.OptionHelper._
import org.slf4j.LoggerFactory
import play.api.libs.json._

/**
  * Json Capability
  */
trait JsonCapability {
  self: Record =>

  protected val logger = LoggerFactory.getLogger(getClass)
  private val fieldMappings = Map(fields.map(f => f.name -> f): _*)

  def fromJson(jsonString: String) = {
    Json.parse(jsonString) match {
      case jo: JsObject => populate(prefix = None, jo)
      case js =>
        logger.info(s"Unhandled JSON value '$js' (${js.getClass.getSimpleName})")
    }
  }

  def toJson: JsObject = {
    val jsValues = fields.map(f => f.name -> f.convertToJson)
    jsValues.foldLeft(Json.obj()) { case (js, (k, v)) => js ++ Json.obj(k -> v) }
  }

  private def populate(prefix: Option[String], jsObject: JsObject) {
    jsObject.value foreach { case (name, js) =>
      logger.info(s"js.path = ${prefix.map(s => s"$s.$name") getOrElse name}")
      fieldMappings.get(name) foreach { field =>
        js match {
          case JsBoolean(value) => field.value = value
          case JsNull => field.value = None
          case JsNumber(value) => field.value = value.toDouble
          case jo: JsObject => populate(prefix.map(s => s"$s.$name") ?? name, jo)
          case JsString(value) => field.value = value
          case unknown =>
            throw new IllegalArgumentException(s"Could not convert type '$unknown' to a Scala value")
        }
      }
    }
  }

}

/**
  * Json Record Companion Object
  */
object JsonCapability {

  /**
    * Json Record Enrichment
    *
    * @param field the given [[Element field]]
    */
  implicit class JsonRecordEnrichment(val field: Element) extends AnyVal {

    def convertToJson: JsValue = {
      field.value map { value =>
        field.`type` match {
          case BOOLEAN => JsBoolean(value == "true")
          case DATE => JsNumber(value.toString.toLong)
          case DOUBLE | FLOAT | INT | LONG => JsNumber(value.toString.toDouble)
          case STRING => JsString(value.toString)
          case unknown =>
            throw new IllegalArgumentException(s"Could not convert type '$unknown' to JSON")
        }
      } getOrElse JsNull
    }
  }

}