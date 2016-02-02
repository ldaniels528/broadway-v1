package com.github.ldaniels528.broadway.core.io.record

import java.util.Date

import com.github.ldaniels528.broadway.core.io.Scope
import com.github.ldaniels528.broadway.core.io.record.DataTypes.{DataType, _}
import com.github.ldaniels528.broadway.core.io.record.Field._
import com.ldaniels528.commons.helpers.OptionHelper._

import scala.util.Try

/**
  * Represents a generic column, field, property or XML element
  * @author lawrence.daniels@gmail.com
  * @param name         the name of the element
  * @param `type`       the data type of the element
  * @param defaultValue the default value of the element
  * @param properties   the properties or attributes of the element
  * @param elements     the child elements of this element
  */
case class Field(name: String,
                 path: String,
                 `type`: DataType = DataTypes.STRING,
                 defaultValue: Option[String] = None,
                 length: Option[Int] = None,
                 properties: Seq[Field] = Nil,
                 elements: Seq[Field] = Nil) {

  def value(implicit scope: Scope): Option[Any] = {
    val result = (defaultValue flatMap {
      case expr: String if expr.contains("{{") => scope.evaluate(expr).convert(`type`)
      case value: String => value.convert(`type`)
      case value => Option(value)
    }) ?? scope.find(path)
    result
  }

  def value_=(newValue: Option[Any])(implicit scope: Scope) = newValue.foreach(scope += path -> _)

}

/**
  * Field Companion Object
  * @author lawrence.daniels@gmail.com
  */
object Field {

  implicit class FieldTypeEnrichment(val value: String) extends AnyVal {

    def convert(`type`: DataType): Option[Any] = {
      `type` match {
        case BOOLEAN => Option(value.toLowerCase == "true")
        case DATE => Try(new Date(value.toLong)).toOption
        case DOUBLE => Try(value.toDouble).toOption
        case FLOAT => Try(value.toFloat).toOption
        case INT => Try(value.toInt).toOption
        case LONG => Try(value.toLong).toOption
        case STRING => Option(value)
        case unknown =>
          throw new IllegalArgumentException(s"Could not convert type '$unknown' to JSON")
      }
    }
  }

}