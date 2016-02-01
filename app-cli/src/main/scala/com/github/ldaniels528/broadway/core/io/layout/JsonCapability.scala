package com.github.ldaniels528.broadway.core.io.layout

import com.github.ldaniels528.broadway.core.io.Scope
import com.github.ldaniels528.broadway.core.io.layout.DataTypes._
import com.github.ldaniels528.broadway.core.io.layout.JsonCapability._
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

  def fromJson(jsonString: String)(implicit scope: Scope): this.type = {
    Json.parse(jsonString) match {
      case jsObject: JsObject => scope ++= toProperties(jsObject)
      case js =>
        logger.info(s"Unhandled JSON value '$js' (${js.getClass.getSimpleName})")
    }
    this
  }

  def toJson(implicit scope: Scope): JsObject = {
    val jsValues = fields.map(f => f.name -> f.convertToJson)
    jsValues.foldLeft(Json.obj()) { case (js, (k, v)) => js ++ Json.obj(k -> v) }
  }

  protected def findFieldInPath(originalFields: Seq[Field], property: String) = {
    val path = property.split("[.]")
    val firstName = path.headOption orDie "Path is empty"
    val firstField = fields.find(_.name == firstName) orDie s"$firstName of path $property not found"

    path.tail.foldLeft(firstField) { case (field, name) =>
      field.elements.find(_.name == name) orDie s"$name of path $property not found"
    }
  }

  protected def toProperties(jsObject: JsObject, prefix: Option[String] = None): List[(String, Any)] = {
    def fullName(name: String) = prefix.map(s => s"$s.$name") getOrElse name

    jsObject.value.foldLeft[List[(String, Any)]](Nil) { case (list, (name, js)) =>
      val result: List[(String, Any)] = js match {
        case JsBoolean(value) => List(fullName(name) -> value)
        case JsNull => Nil
        case JsNumber(value) => List(fullName(name) -> value.toDouble)
        case jo: JsObject => toProperties(jo, prefix = fullName(name))
        case unknown =>
          throw new IllegalArgumentException(s"Could not convert property '$name' type '$unknown' to a Scala value")
      }
      result ::: list
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
    * @param field the given [[Field field]]
    */
  implicit class JsonRecordEnrichment(val field: Field) extends AnyVal {

    def convertToJson(implicit scope: Scope): JsValue = {
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