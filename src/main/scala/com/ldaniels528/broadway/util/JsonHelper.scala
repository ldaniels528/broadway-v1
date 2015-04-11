package com.ldaniels528.broadway.util

import net.liftweb.json.JsonAST.JValue
import net.liftweb.json._

import scala.util.{Failure, Success, Try}

/**
 * JSON Helper Utility
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
object JsonHelper {
  implicit val formats = DefaultFormats

  def decompose(value: Any): JValue = Extraction.decompose(value)

  /**
   * Re-formats the given JSON string as a "pretty" version of the JSON string
   * @param jsonString the given JSON string
   * @return a "pretty" version of the JSON string
   */
  def makePretty(jsonString: String): String = {
    Try(toJson(jsonString)) match {
      case Success(js) => pretty(render(js))
      case Failure(e) => jsonString
    }
  }

  /**
   * Transforms the given JSON string into the specified type
   * @param jsonString the given JSON string (e.g. { "symbol":"AAPL", "price":"115.44" })
   * @param manifest the implicit [[Manifest]]
   * @tparam T the specified type
   * @return an instance of the specified type
   */
  def transform[T](jsonString: String)(implicit manifest: Manifest[T]): T = parse(jsonString).extract[T]

  /**
   * Converts the given string into a JSON value
   * @param jsonString the given JSON string
   * @return the resultant [[JValue]]
   */
  def toJson(jsonString: String): JValue = parse(jsonString)

  def toJson[T](results: Seq[T]): JValue = Extraction.decompose(results)

  def toJsonString(bean: AnyRef): String = compact(render(Extraction.decompose(bean)))


}

